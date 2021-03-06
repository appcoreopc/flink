/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.api.scala.batch.sql

import org.apache.flink.api.scala._
import org.apache.flink.table.api.scala.batch.utils.TableProgramsTestBase
import org.apache.flink.table.api.scala._
import org.apache.flink.api.scala.util.CollectionDataSets
import TableProgramsTestBase.TableConfigMode
import org.apache.flink.types.Row
import org.apache.flink.table.api.{TableEnvironment, TableException}
import org.apache.flink.test.util.MultipleProgramsTestBase.TestExecutionMode
import org.apache.flink.test.util.TestBaseUtils
import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import scala.collection.JavaConverters._

@RunWith(classOf[Parameterized])
class AggregationsITCase(
    mode: TestExecutionMode,
    configMode: TableConfigMode)
  extends TableProgramsTestBase(mode, configMode) {

  @Test
  def testAggregationTypes(): Unit = {

    val env = ExecutionEnvironment.getExecutionEnvironment
    val tEnv = TableEnvironment.getTableEnvironment(env, config)

    val sqlQuery = "SELECT sum(_1), min(_1), max(_1), count(_1), avg(_1) FROM MyTable"

    val ds = CollectionDataSets.get3TupleDataSet(env)
    tEnv.registerDataSet("MyTable", ds)

    val result = tEnv.sql(sqlQuery)

    val expected = "231,1,21,21,11"
    val results = result.toDataSet[Row].collect()
    TestBaseUtils.compareResultAsText(results.asJava, expected)
  }

  @Test
  def testTableAggregation(): Unit = {

    val env = ExecutionEnvironment.getExecutionEnvironment
    val tEnv = TableEnvironment.getTableEnvironment(env, config)

    val sqlQuery = "SELECT sum(_1) FROM MyTable"

    val ds = CollectionDataSets.get3TupleDataSet(env).toTable(tEnv)
    tEnv.registerTable("MyTable", ds)

    val result = tEnv.sql(sqlQuery)

    val expected = "231"
    val results = result.toDataSet[Row].collect()
    TestBaseUtils.compareResultAsText(results.asJava, expected)
  }

  @Test
  def testDataSetAggregation(): Unit = {

    val env = ExecutionEnvironment.getExecutionEnvironment
    val tEnv = TableEnvironment.getTableEnvironment(env, config)

    val sqlQuery = "SELECT sum(_1) FROM MyTable"

    val ds = CollectionDataSets.get3TupleDataSet(env)
    tEnv.registerDataSet("MyTable", ds)

    val result = tEnv.sql(sqlQuery)

    val expected = "231"
    val results = result.toDataSet[Row].collect()
    TestBaseUtils.compareResultAsText(results.asJava, expected)
  }

  @Test
  def testWorkingAggregationDataTypes(): Unit = {

    val env = ExecutionEnvironment.getExecutionEnvironment
    val tEnv = TableEnvironment.getTableEnvironment(env, config)

    val sqlQuery =
      "SELECT avg(_1), avg(_2), avg(_3), avg(_4), avg(_5), avg(_6), count(_7), " +
      "  sum(CAST(_6 AS DECIMAL))" +
      "FROM MyTable"

    val ds = env.fromElements(
      (1: Byte, 1: Short, 1, 1L, 1.0f, 1.0d, "Hello"),
      (2: Byte, 2: Short, 2, 2L, 2.0f, 2.0d, "Ciao"))
    tEnv.registerDataSet("MyTable", ds)

    val result = tEnv.sql(sqlQuery)

    val expected = "1,1,1,1,1.5,1.5,2,3.0"
    val results = result.toDataSet[Row].collect()
    TestBaseUtils.compareResultAsText(results.asJava, expected)
  }

  @Test
  def testTableWorkingAggregationDataTypes(): Unit = {

    val env = ExecutionEnvironment.getExecutionEnvironment
    val tEnv = TableEnvironment.getTableEnvironment(env, config)

    val sqlQuery = "SELECT avg(a), avg(b), avg(c), avg(d), avg(e), avg(f), count(g)" +
      "FROM MyTable"

    val ds = env.fromElements(
      (1: Byte, 1: Short, 1, 1L, 1.0f, 1.0d, "Hello"),
      (2: Byte, 2: Short, 2, 2L, 2.0f, 2.0d, "Ciao")).toTable(tEnv, 'a, 'b, 'c, 'd, 'e, 'f, 'g)
    tEnv.registerTable("MyTable", ds)

    val result = tEnv.sql(sqlQuery)

    val expected = "1,1,1,1,1.5,1.5,2"
    val results = result.toDataSet[Row].collect()
    TestBaseUtils.compareResultAsText(results.asJava, expected)
  }

  @Test
  def testTableProjection(): Unit = {

    val env = ExecutionEnvironment.getExecutionEnvironment
    val tEnv = TableEnvironment.getTableEnvironment(env, config)

    val sqlQuery = "SELECT avg(a), sum(a), count(a), avg(b), sum(b) " +
      "FROM MyTable"

    val ds = env.fromElements((1: Byte, 1: Short), (2: Byte, 2: Short)).toTable(tEnv, 'a, 'b)
    tEnv.registerTable("MyTable", ds)

    val result = tEnv.sql(sqlQuery)

    val expected = "1,3,2,1,3"
    val results = result.toDataSet[Row].collect()
    TestBaseUtils.compareResultAsText(results.asJava, expected)
  }

  @Test
  def testTableAggregationWithArithmetic(): Unit = {

    val env = ExecutionEnvironment.getExecutionEnvironment
    val tEnv = TableEnvironment.getTableEnvironment(env, config)

    val sqlQuery = "SELECT avg(a + 2) + 2, count(b) + 5 " +
      "FROM MyTable"

    val ds = env.fromElements((1f, "Hello"), (2f, "Ciao")).toTable(tEnv, 'a, 'b)
    tEnv.registerTable("MyTable", ds)

    val result = tEnv.sql(sqlQuery)

    val expected = "5.5,7"
    val results = result.toDataSet[Row].collect()
    TestBaseUtils.compareResultAsText(results.asJava, expected)
  }

  @Test
  def testAggregationWithTwoCount(): Unit = {

    val env = ExecutionEnvironment.getExecutionEnvironment
    val tEnv = TableEnvironment.getTableEnvironment(env, config)

    val sqlQuery = "SELECT count(_1), count(_2) FROM MyTable"

    val ds = env.fromElements((1f, "Hello"), (2f, "Ciao")).toTable(tEnv)
    tEnv.registerTable("MyTable", ds)

    val result = tEnv.sql(sqlQuery)

    val expected = "2,2"
    val results = result.toDataSet[Row].collect()
    TestBaseUtils.compareResultAsText(results.asJava, expected)
  }


  @Test
  def testAggregationAfterProjection(): Unit = {

    val env = ExecutionEnvironment.getExecutionEnvironment
    val tEnv = TableEnvironment.getTableEnvironment(env, config)

    val sqlQuery = "SELECT avg(a), sum(b), count(c) FROM " +
      "(SELECT _1 as a, _2 as b, _3 as c FROM MyTable)"

    val ds = env.fromElements(
      (1: Byte, 1: Short, 1, 1L, 1.0f, 1.0d, "Hello"),
      (2: Byte, 2: Short, 2, 2L, 2.0f, 2.0d, "Ciao")).toTable(tEnv)
    tEnv.registerTable("MyTable", ds)

    val result = tEnv.sql(sqlQuery)

    val expected = "1,3,2"
    val results = result.toDataSet[Row].collect()
    TestBaseUtils.compareResultAsText(results.asJava, expected)
  }

  @Test(expected = classOf[TableException])
  def testDistinctAggregate(): Unit = {

    val env = ExecutionEnvironment.getExecutionEnvironment
    val tEnv = TableEnvironment.getTableEnvironment(env, config)

    val sqlQuery = "SELECT sum(_1) as a, count(distinct _3) as b FROM MyTable"

    val ds = CollectionDataSets.get3TupleDataSet(env)
    tEnv.registerDataSet("MyTable", ds)

    // must fail. distinct aggregates are not supported
    tEnv.sql(sqlQuery).toDataSet[Row]
  }

  @Test(expected = classOf[TableException])
  def testGroupedDistinctAggregate(): Unit = {

    val env = ExecutionEnvironment.getExecutionEnvironment
    val tEnv = TableEnvironment.getTableEnvironment(env, config)

    val sqlQuery = "SELECT _2, avg(distinct _1) as a, count(_3) as b FROM MyTable GROUP BY _2"

    val ds = CollectionDataSets.get3TupleDataSet(env)
    tEnv.registerDataSet("MyTable", ds)

    // must fail. distinct aggregates are not supported
    tEnv.sql(sqlQuery).toDataSet[Row]
  }

  @Test(expected = classOf[TableException])
  def testGroupingSetAggregate(): Unit = {

    val env = ExecutionEnvironment.getExecutionEnvironment
    val tEnv = TableEnvironment.getTableEnvironment(env, config)

    val sqlQuery = "SELECT _2, _3, avg(_1) as a FROM MyTable GROUP BY GROUPING SETS (_2, _3)"

    val ds = CollectionDataSets.get3TupleDataSet(env)
    tEnv.registerDataSet("MyTable", ds)

    // must fail. grouping sets are not supported
    tEnv.sql(sqlQuery).toDataSet[Row]
  }

  @Test
  def testAggregateEmptyDataSets(): Unit = {

    val env = ExecutionEnvironment.getExecutionEnvironment
    val tEnv = TableEnvironment.getTableEnvironment(env, config)

    val sqlQuery = "SELECT avg(a), sum(a), count(b) " +
      "FROM MyTable where a = 4 group by a"

    val sqlQuery2 = "SELECT avg(a), sum(a), count(b) " +
      "FROM MyTable where a = 4"

    val sqlQuery3 = "SELECT avg(a), sum(a), count(b) " +
      "FROM MyTable"

    val ds = env.fromElements(
      (1: Byte, 1: Short),
      (2: Byte, 2: Short))
      .toTable(tEnv, 'a, 'b)

    tEnv.registerTable("MyTable", ds)

    val result = tEnv.sql(sqlQuery)
    val result2 = tEnv.sql(sqlQuery2)
    val result3 = tEnv.sql(sqlQuery3)

    val results = result.toDataSet[Row].collect()
    val expected = Seq.empty
    val results2 =  result2.toDataSet[Row].collect()
    val expected2 = "null,null,0"
    val results3 = result3.toDataSet[Row].collect()
    val expected3 = "1,3,2"

    assert(results.equals(expected),
      "Empty result is expected for grouped set, but actual: " + results)
    TestBaseUtils.compareResultAsText(results2.asJava, expected2)
    TestBaseUtils.compareResultAsText(results3.asJava, expected3)
  }
}
