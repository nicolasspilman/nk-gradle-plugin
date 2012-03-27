/*
* Copyright 2011 Brian Sletten
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/

package net.bosatsu.util.netkernel

class PackageDependencyHelper {

   static def equalityMapping = [">":"gt",">=":"gte","=":"e","<=":"lte","<":"lt"]
   def dependencies = []
   def closure
   
   PackageDependencyHelper(Closure closure) {
      this.closure = closure
      this.closure.delegate = this
   }
   
   def addPackageDependency = { deptype, map ->
      def response = [:]

      response."name" = map."name"
      response."deptype" = deptype

      // Parse version string
      def m = (map."version" =~ /^(.*?)\s*(\d.*)$/)
      response."equality" = equalityMapping[m[0][1]]
      response."version" = m[0][2]

      dependencies.add response
   }

   // Process the actual dependency directives
   def depends = addPackageDependency.curry("depends")
   def suggests = addPackageDependency.curry("suggests")
   def recommends = addPackageDependency.curry("recommends")
   def conflicts = addPackageDependency.curry("conflicts")
   def replaces = addPackageDependency.curry("replaces")
   
   def processDependencies = {
      this.closure()
      dependencies
   }
}
