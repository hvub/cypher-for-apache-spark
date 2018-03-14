/*
 * Copyright (c) 2016-2018 "Neo4j, Inc." [https://neo4j.com]
 *
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
package org.opencypher.okapi.relational.impl.flat

import org.opencypher.okapi.api.types.CTNode
import org.opencypher.okapi.ir.api.util.FreshVariableNamer
import org.opencypher.okapi.test.BaseTestSuite

class FreshVariableNamerTest extends BaseTestSuite {
  test("generates prefixed name") {
    FreshVariableNamer("Foobar", CTNode).name should equal("  Foobar")
  }
}