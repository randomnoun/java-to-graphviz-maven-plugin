# java-to-graphviz-maven-plugin

This plugin generates graphviz sources and images from java source code.

## Overview

This plugin can be used to help document specific classes and interfaces of a larger software project.

It uses the [java-to-graphviz](https://github.com/randomnoun/java-to-graphviz) tool to generate the graphviz diagrams. This diagram 
will read java source code, and generate graphviz 'dot' source, which can be converted to PNGs and included in the maven-generated
site documentation.

The shape of the diagrams can be modified using specially-constructed comments in the source file; see the 
[java-to-graphviz](https://github.com/randomnoun/java-to-graphviz) README for details.

## Example pom.xml

Here's a minimal pom.xml that generates some images from java source code.

The images can be included in the javadoc by adding `<img src="doc-files/Classname.png"/>` tags to the javadoc.

<pre>
&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;project xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd"&gt;
  &lt;modelVersion&gt;4.0.0&lt;/modelVersion&gt;

  &lt;groupId&gt;com.example.contrived&lt;/groupId&gt;
  &lt;artifactId&gt;contrived-web&lt;/artifactId&gt;
  &lt;packaging&gt;war&lt;/packaging&gt;
  &lt;version&gt;0.0.2-SNAPSHOT&lt;/version&gt;

  &lt;build&gt;
    &lt;plugins&gt;
    
      &lt;!-- 
        copies additional javadoc files from src/main/javadoc 
        to target/javadoc-resources ( which will also contain the generated images )
      --&gt;
      &lt;plugin&gt;
        &lt;artifactId&gt;maven-resources-plugin&lt;/artifactId&gt;
        &lt;version&gt;2.6&lt;/version&gt;
        &lt;executions&gt;
          &lt;execution&gt;
            &lt;id&gt;copy-javadoc-resources&lt;/id&gt;
            &lt;phase&gt;pre-site&lt;/phase&gt;
            &lt;goals&gt;
              &lt;goal&gt;copy-resources&lt;/goal&gt;
            &lt;/goals&gt;
            &lt;configuration&gt;
              &lt;resources&gt;
                &lt;resource&gt;
                  &lt;directory&gt;src/main/javadoc&lt;/directory&gt;
                &lt;/resource&gt;
              &lt;/resources&gt;
              &lt;outputDirectory&gt;${basedir}/target/javadoc-resources&lt;/outputDirectory&gt;
            &lt;/configuration&gt;
          &lt;/execution&gt;
        &lt;/executions&gt;
      &lt;/plugin&gt;

      &lt;!-- 
        generate graphviz diagrams and images
      --&gt;
      &lt;plugin&gt;
        &lt;groupId&gt;com.randomnoun.maven.plugins&lt;/groupId&gt;
        &lt;artifactId&gt;java-to-graphviz-maven-plugin&lt;/artifactId&gt;
        &lt;version&gt;1.0.4&lt;/version&gt;
        &lt;executions&gt;
          &lt;execution&gt;
            &lt;id&gt;java-to-graphviz&lt;/id&gt;
            &lt;phase&gt;pre-site&lt;/phase&gt;
            &lt;goals&gt;
              &lt;goal&gt;java-to-graphviz&lt;/goal&gt;
            &lt;/goals&gt;
            &lt;configuration&gt;
              &lt;fileset&gt;
                &lt;includes&gt;
                  &lt;include&gt;com/example/contrived/web/action/**.java&lt;/include&gt;
                &lt;/includes&gt;
                &lt;directory&gt;${project.basedir}/src/main/java&lt;/directory&gt;
              &lt;/fileset&gt;
              &lt;outputDirectory&gt;${project.basedir}/target/javadoc-resources&lt;/outputDirectory&gt;
              &lt;outputFilenamePattern&gt;{directory}/doc-files/{basename}.dot&lt;/outputFilenamePattern&gt;
              &lt;invokeGraphviz&gt;true&lt;/invokeGraphviz&gt;
              &lt;graphvizExecutable&gt;C:\Program Files\Graphviz\bin\dot.exe&lt;/graphvizExecutable&gt;
              &lt;verbose&gt;false&lt;/graphvizExecutable&gt;
            &lt;/verbose&gt;
          &lt;/execution&gt;
        &lt;/executions&gt;
      &lt;/plugin&gt;
    &lt;/plugins&gt;

  &lt;/build&gt;

  &lt;reporting&gt;
    &lt;plugins&gt;

      &lt;!-- 
        site javadoc
        note javadocDirectory here is the target/javadoc-resources directory written to above
        which also includes src/main/javadoc
      --&gt;
      &lt;plugin&gt;
        &lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;
        &lt;artifactId&gt;maven-javadoc-plugin&lt;/artifactId&gt;
        &lt;version&gt;3.3.1&lt;/version&gt;
        &lt;configuration&gt;
          &lt;source&gt;11&lt;/source&gt;
          &lt;failOnError&gt;false&lt;/failOnError&gt;
          &lt;stylesheetfile&gt;${project.basedir}/target/javadoc/stylesheet.css&lt;/stylesheetfile&gt;
          &lt;javadocDirectory&gt;${project.basedir}/target/javadoc-resources&lt;/javadocDirectory&gt;

          &lt;docfilessubdirs&gt;true&lt;/docfilessubdirs&gt;
          &lt;author&gt;true&lt;/author&gt;
          &lt;linksource&gt;true&lt;/linksource&gt;
          &lt;doclint&gt;none&lt;/doclint&gt;
        &lt;/configuration&gt;
        &lt;reportSets&gt;
          &lt;reportSet&gt;
            &lt;reports&gt;
              &lt;report&gt;javadoc&lt;/report&gt;
            &lt;/reports&gt;
          &lt;/reportSet&gt;
        &lt;/reportSets&gt;
      &lt;/plugin&gt;

    &lt;/plugins&gt;
  &lt;/reporting&gt;

&lt;/project&gt;
</pre>

## Is there a blog article about this project ?

Yes there is: http://www.randomnoun.com/wp/2021/12/11/flowcharts-r-us/

## Licensing

java-to-graphviz-maven-plugin is licensed under the BSD 2-clause license.

