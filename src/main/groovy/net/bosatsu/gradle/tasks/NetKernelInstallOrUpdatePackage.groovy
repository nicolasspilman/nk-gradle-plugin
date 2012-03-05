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

package net.bosatsu.gradle.tasks

import org.gradle.api.DefaultTask

class NetKernelInstallOrUpdatePackage extends DefaultTask {

   def packageName
   def packageVersion

   static def BASE_URI = "netkernelbaseuri"

   @org.gradle.api.tasks.TaskAction
   void installOrUpdate() {
      def baseUrl = project.getProperty(BASE_URI) + "/tools/apposite/unattended/v1"

      if(project.hasProperty(BASE_URI)) {
         if(packageInstalled(baseUrl)) {
            println "Package $packageName already installed.  Invoking update."
            println new URL("$baseUrl/change?update=$packageName").text
         } else {
            println "Package $packageName not found.  Invoking install."
            println new URL("$baseUrl/change?install=$packageName").text
         }

         // Insert loop here to keep polling installed endpoint
         5.times {
            sleep(5000)
            println "Determining if $packageName is installed"
            if(packageInstalled(baseUrl)) {
               println "Package $packageName installed succesfully."
               return
            }
         }
      }
   }

   def packageInstalled = { baseUrl ->
      def status = new URL("$baseUrl/installed?match=$packageName").text
      def statusXml = new XmlSlurper().parseText(status)
      statusXml.row?.INSTALLED.text() == "true"
   }
}
