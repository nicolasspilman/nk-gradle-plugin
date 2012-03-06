package net.bosatsu.util.netkernel

import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient



class AppositeHelper {

   HttpClient httpClient = new DefaultHttpClient()
   def baseUrl

   AppositeHelper(def netkernelbaseuri) {
      this.baseUrl = "$netkernelbaseuri/tools/apposite/unattended/v1"
   }


   def synchronize() {
      HttpGet httpGet = new HttpGet("$baseUrl/synchronize")
      HttpResponse response = httpClient.execute(httpGet)
      println response.entity.content.text
   }

   def isInstalled(String packageName) {
      isInstalled(packageName, null)
   }

   def isInstalled(String packageName, String packageVersion) {
      HttpGet httpGet = new HttpGet("$baseUrl/installed?match=$packageName")
      HttpResponse response = httpClient.execute(httpGet)
      def responseXml = new XmlSlurper().parse(response.entity.content)

      if(packageVersion) {
         responseXml.row?.INSTALLED.text() == "true" && responseXml.row?.VP.text() == packageVersion
      } else {
         responseXml.row?.INSTALLED.text() == "true"
      }
   }

   def install(String packageName, String packageVersion, int attempts, int interval) {
      performChangeAndWait("install", packageName, packageVersion, attempts, interval)
   }

   def update(String packageName, String packageVersion, int attempts, int interval) {
      performChangeAndWait("update", packageName, packageVersion, attempts, interval)
   }

   private def performChangeAndWait(String action, String packageName, String packageVersion, int attempts, int interval) {
      HttpGet httpGet = new HttpGet("$baseUrl/$action?install=$packageName")
      HttpResponse response = httpClient.execute(httpGet)
      println response.entity.content.text

      boolean installed = false
      
      while(!installed && attempts) {
         sleep(interval)
         if(isInstalled(packageName, packageVersion)) {
            println "Package [name: $packageName, version: $packageVersion] has been commissioned."
            return
         }
         attempts--
      }
   }
}
