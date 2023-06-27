package com.numbytes.maven.plugins;
/*
 * The MIT License
 *
 * Copyright (c) 2004
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import javax.json.*;
import java.util.Random;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.codehaus.plexus.util.FileUtils;


/**
 * Copy files during build
 *
 * @author <a href="jose.pujante@numbytes.com">Pujante Jose Luis</a>
 * @version 1.1
 */
@Mojo(name = "copy", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class CopyMojo
        extends AbstractMojo {
    /**
     * The file which has to be copied
     *
     * @since 1.0
     */
    @Parameter(required = false)
    private File sourceFile;
    /**
     * The target file to which the file should be copied(this shouldn't be a directory but a file which does or does not exist)
     *
     * @since 1.0
     */
    @Parameter(required = false)
    private File destinationFile;

    /**
     * The asset name
     *
     * @since 1.0
     */
    @Parameter(required = false)
    private String assetName;
    /**
     * The asset value
     *
     * @since 1.0
     */
    @Parameter(required = false)
    private String assetValue;

    /**
     * Collection of FileSets to work on (FileSet contains sourceFile and destinationFile). See <a href="./usage.html">Usage</a> for details.
     *
     * @since 1.0
     */
    @Parameter(required = false)
    private List<FileSet> fileSets;

    /**
     * Overwrite files
     *
     * @since 1.0
     */
    @Parameter(property = "copy.overWrite", defaultValue = "true")
    boolean overWrite;

    /**
     * Ignore File Not Found errors during incremental build
     *
     * @since 1.0
     */
    @Parameter(property = "copy.ignoreFileNotFoundOnIncremental", defaultValue = "true")
    boolean ignoreFileNotFoundOnIncremental;

    /**
     * Collection of TemplatesBundle to work on.
     *
     * @since 1.0
     */
    @Parameter(required = false)
    private TemplateBundle templatesBundle;
//
//    /**
//     * Template tag
//     *
//     * @since 1.0
//     */
//    @Parameter(property = "copy.templatesBundleTag", required = false, defaultValue = "")
//    private String templatesBundleTag;
//
//    /**
//     * Destination path for the template bundle
//     *
//     * @since 1.0
//     */
//    @Parameter(property = "copy.templatesBundleFile", required = false, defaultValue = "")
//    private String templatesBundleFile;

    /**
     * The assets file with the results
     *
     * @since 1.0
     */
    @Parameter(property = "copy.fileOutput", defaultValue = "assets.json")
    private String fileOutput;

    /**
     * The assets file with the results
     *
     * @since 1.0
     */
    @Parameter(property = "copy.fileJsOutput", defaultValue = "assets.js")
    private String fileJsOutput;

    /**
     * The assets file with the results
     *
     * @since 1.0
     */
    private JsonObjectBuilder assetsBuilder;

    /**
     * @since 1.0
     */
    @Component
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    public void execute() throws MojoExecutionException {
        getLog().info("+-----------------------------------------------+");
        getLog().info("| Executing the copy-assets-maven-plugin        |");
        getLog().info("| Author: jose.pujante@numbytes.com |");
        getLog().info("+-----------------------------------------------+");

        if (fileSets != null && fileSets.size() > 0) {
            assetsBuilder = Json.createObjectBuilder();
            for (FileSet fileSet : fileSets) {
                File srcFile = fileSet.getSourceFile();
                File destFile = fileSet.getDestinationFile();
                String assetName = fileSet.getAssetName();
                String assetValue = fileSet.getAssetValue();
                if (srcFile != null && destFile != null && assetName != null && assetValue != null) {
                    copy(srcFile, destFile);
                    assetsBuilder.add(assetName, assetValue);
                } else {
                    getLog().error("Invalid FileSet");
                    getLog().error("sourceFile: " + srcFile);
                    getLog().error("destinationFile: " + destFile);
                    getLog().error("assetName: " + assetName);
                    getLog().error("assetValue: " + assetValue);
                }
            }
        } else if (sourceFile != null) {
            copy(sourceFile, destinationFile);
            assetsBuilder = Json.createObjectBuilder().add(assetName, assetValue);
        } else {
            getLog().info("No Files to process");
        }

        writeOutput();

        //Generate a single file
        if (templatesBundle!=null) {
            String templatesBundleFile = templatesBundle.getDestination();
            String templatesBundleTag = templatesBundle.getTag();
            List<String> templatesBundleInjectFile = templatesBundle.getInjectFiles();
            if ( templatesBundleFile==null || templatesBundleFile.isEmpty() ) {
                getLog().warn("No destination bundle defined. Nothing to do");
            } else {
                getLog().info("Bundle file [" + templatesBundleFile + "]");
                getLog().info("Bundle tag [" + templatesBundleTag + "]");
                try {
                    getLog().info("Opening file [" + templatesBundleFile + "]");
                    Writer w = openFile(templatesBundleFile, false);
                    if (fileSets != null && fileSets.size() > 0) {
                        String templates = "";
                        for (FileSet fileSet : fileSets) {
                            String tmpl = "";
                            File destFile = fileSet.getDestinationFile();
                            if (destFile != null) {
                                String content = new String(Files.readAllBytes(Paths.get(destFile.getAbsolutePath())));
                                if (content.startsWith("<script")) {
                                    tmpl += content;
                                } else {
                                    tmpl += "<script type=\"text/ng-template\" id=\"" + destFile.getName() + "\">\n";
                                    tmpl += content;
                                    tmpl += "</script>\n";
                                }
                                w.write(tmpl);
                                templates += tmpl;
                            }
                        }
                        w.flush();
                        w.close();
                        if ( templatesBundleTag!=null && !templatesBundleTag.isEmpty() ) {
                            if (!templates.isEmpty()) {
                                for (String tbdf : templatesBundleInjectFile) {
                                    File p = new File(tbdf);
                                    if ( !p.exists() ) {
                                        getLog().error("Templates file not found");
                                        getLog().error(tbdf);
                                        throw new MojoExecutionException("File not found\"");
                                    }
                                    try {
                                        String content = new String(Files.readAllBytes(Paths.get(tbdf)));
                                        content = content.replace(templatesBundleTag, templates);
                                        Writer tbdfWriter = openFile(tbdf, false);
                                        tbdfWriter.write(content);
                                        tbdfWriter.close();
                                        getLog().info("Bundle injected file [" + tbdf + "]");
                                    } catch (Exception e) {
                                        getLog().error("Problem writing to the file (" + tbdf + ")");
                                        writeException(e);
                                        throw new MojoExecutionException("An error occurred");
                                    }
                                }
                            } else {
                                getLog().warn("No templates detected. Nothing to do");
                            }
                        }
                    }
                } catch (Exception e) {
                    getLog().error("Problem writing to the file (" + templatesBundleFile + ")");
                    writeException(e);
                    throw new MojoExecutionException("An error occurred (1)");
                }
            }
        }
    }

    private void writeException(Throwable e) {
        for (StackTraceElement element : e.getStackTrace()) {
            getLog().error(element.toString());
        }
    }

    private Writer openFile(String path, Boolean appendMode) throws IOException {
        File f = new File(path);
        if( f.exists() ) {
            if (!appendMode) {
                getLog().info("Truncating file [" + path + "]");
                FileWriter fw = new FileWriter(path, false);
                fw.close();
            }
        }
        getLog().info("Opening file [" + path + "]");
        FileOutputStream fos = new FileOutputStream(f);
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        return new BufferedWriter(osw);
    }
    private void writeOutput() throws MojoExecutionException {
        JsonObject assetsJson = assetsBuilder.build();
        String jsonContent = assetsJson.toString();
        // String jsonContent = assetsBuilder.build().toString();
        try {
            getLog().info("Writing output assets file (" + fileOutput + ")");
            Writer w = openFile(fileOutput, false);
            w.write(jsonContent);
            w.close();

            getLog().info("Writing output assets Javascript file (" + fileJsOutput + ")");
            w = openFile(fileJsOutput,false);
            w.write("if (typeof(ASSETS_INFO) != 'undefined') {");
            Random rand = new Random();
            int x = rand.nextInt(1000);
            String partialAssetsInfoName = "ASSETS_INFO_" + String.valueOf(x);
            w.write(partialAssetsInfoName + "=" + jsonContent  + ";");
            w.write("ASSETS_INFO=merge_options(ASSETS_INFO, " + partialAssetsInfoName + ");");
            w.write("if (window.hasOwnProperty('DEBUG') || (window.hasOwnProperty('GLOBALITY_SETTING') && GLOBALITY_SETTING.DEBUG)) {");
            w.write("console.log('Adding assets');");
            w.write("}");
            // w.write("ASSETS_INFO['" + appContext + "']=" + jsonContent + ";");
            // w.write("console.log('Adding assets for context:' + '" + appContext + "');");
            w.write("} else {");
            w.write("ASSETS_INFO=" + jsonContent  + ";");
            w.write("if (window.hasOwnProperty('DEBUG') || (window.hasOwnProperty('GLOBALITY_SETTING') && GLOBALITY_SETTING.DEBUG)) {");
            w.write("console.log('Creating assets');");
            w.write("}");
            // w.write("ASSETS_INFO={};");
            // w.write("ASSETS_INFO['" + appContext + "']=" + jsonContent + ";");
            // w.write("console.log('Creating assets for context:' + '" + appContext + "');");
            w.write("}");
            w.close();
        } catch (Exception e) {
            getLog().error("Problem writing to the file (" + fileOutput + ")");
            writeException(e);
            throw new MojoExecutionException("An error occurred (2)");
        }
    }

    private void copy(File srcFile, File destFile) throws MojoExecutionException {
        if (!srcFile.exists()) {
            if (ignoreFileNotFoundOnIncremental && buildContext.isIncremental()) {
                getLog().warn("sourceFile " + srcFile.getAbsolutePath() + " not found during incremental build");
            } else {
                getLog().error("sourceFile " + srcFile.getAbsolutePath() + " does not exist");
            }
        } else if (srcFile.isDirectory()) {
            getLog().error("sourceFile " + srcFile.getAbsolutePath() + " is not a file");
        } else if (destFile == null) {
            getLog().error("destinationFile not specified");
        } else if (destFile.exists() && destFile.isFile() && !overWrite) {
            getLog().error(destFile.getAbsolutePath() + " already exists and overWrite not set");
        } else {
            try {
                if (buildContext.isIncremental() && destFile.exists() && !buildContext.hasDelta(srcFile) && FileUtils.contentEquals(srcFile, destFile)) {
                    getLog().info("No changes detected in " + srcFile.getAbsolutePath());
                    return;
                }
                FileUtils.copyFile(srcFile, destFile);
                getLog().info("Copied " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
                buildContext.refresh(destFile);
            } catch (Exception e) {
                throw new MojoExecutionException("could not copy " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
            }
        }
    }
}
