/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.bloomfilter;

import com.facebook.presto.operator.aggregation.AggregationFunction;
import com.facebook.presto.operator.aggregation.CombineFunction;
import com.facebook.presto.operator.aggregation.InputFunction;
import com.facebook.presto.operator.aggregation.OutputFunction;
import com.facebook.presto.spi.block.BlockBuilder;
import com.facebook.presto.spi.type.StandardTypes;
import com.facebook.presto.type.SqlType;
import io.airlift.slice.Slice;

import static com.facebook.presto.spi.type.StandardTypes.VARCHAR;

@AggregationFunction(value = "bloom_filter")
public class BloomFilterAggregation
{
    private BloomFilterAggregation()
    {
    }

    @InputFunction
    public static void input(
            BloomFilterState state,
            @SqlType(VARCHAR) Slice slice)
    {
        BloomFilter bf = getOrCreateBloomFilter(state, BloomFilter.DEFAULT_BLOOM_FILTER_EXPECTED_INSERTIONS, BloomFilter.DEFAULT_BLOOM_FILTER_FALSE_POSITIVE_PERCENTAGE);
        // Note: do not update the memory size as this is constant to our bloom filter implementation
        bf.put(slice);
    }

    @InputFunction
    public static void input(
            BloomFilterState state,
            @SqlType(VARCHAR) Slice slice,
            @SqlType(StandardTypes.BIGINT) long expectedInsertions)
    {
        BloomFilter bf = getOrCreateBloomFilter(state, (int) expectedInsertions, BloomFilter.DEFAULT_BLOOM_FILTER_FALSE_POSITIVE_PERCENTAGE);
        // Note: do not update the memory size as this is constant to our bloom filter implementation
        bf.put(slice);
    }

    @InputFunction
    public static void input(
            BloomFilterState state,
            @SqlType(VARCHAR) Slice slice,
            @SqlType(StandardTypes.BIGINT) long expectedInsertions,
            @SqlType(StandardTypes.DOUBLE) double falsePositivePercentage)
    {
        BloomFilter bf = getOrCreateBloomFilter(state, (int) expectedInsertions, falsePositivePercentage);
        // Note: do not update the memory size as this is constant to our bloom filter implementation
        bf.put(slice);
    }

    private static BloomFilter getOrCreateBloomFilter(BloomFilterState state, int expectedInsertions, double falsePositivePercentage)
    {
        BloomFilter bf = state.getBloomFilter();
        if (bf == null) {
            bf = BloomFilter.newInstance(expectedInsertions, falsePositivePercentage);
            state.setBloomFilter(bf);
            state.addMemoryUsage(bf.estimatedInMemorySize());
        }
        return bf;
    }

    @CombineFunction
    public static void combine(BloomFilterState state, BloomFilterState otherState)
    {
        state.setBloomFilter(state.getBloomFilter().putAll(otherState.getBloomFilter()));
    }

    @OutputFunction(BloomFilterType.TYPE)
    public static void output(BloomFilterState state, BlockBuilder out)
    {
        BloomFilterType.BLOOM_FILTER.writeSlice(out, state.getBloomFilter().serialize());
    }
}
