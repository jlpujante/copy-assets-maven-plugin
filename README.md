# copy-assets-maven-plugin

## How to use the plugin

```
<plugin>
    <groupId>com.numbytes.maven.plugins</groupId>
    <artifactId>copy-assets-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <id>html-template-assets</id>
            <phase>process-resources</phase>
            <goals>
                <goal>copy</goal>
            </goals>
            <configuration>
                <fileOutput>${path_to}/assets.json</fileOutput>
                <fileJsOutput>${path_to}/assets.js</fileJsOutput>
                <fileSets>
                    <fileSet>
                        <assetName>/${js.folder}/app/components/numbytesTable/numbytesTable.htm</assetName>
                        <assetValue>${maven.build.timestamp}_numbytesTable.htm</assetValue>
                        <sourceFile>${project.ui.path}/js/app/components/numbytesTable/numbytesTable.htm</sourceFile>
                        <destinationFile>${project.target.path}/${assets.folder}/${maven.build.timestamp}_numbytesTable.htm</destinationFile>
                    </fileSet>
                    ...
                </fileSets>
            </configuration>
        </execution>
    </executions>
</plugin>
```
