package com.randomnoun.maven.plugin.javaToGraphviz;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.utils.io.DirectoryScanner;

import com.randomnoun.build.javaToGraphviz.JavaToGraphviz;
import com.randomnoun.common.ProcessUtil;
import com.randomnoun.common.ProcessUtil.ProcessException;
import com.randomnoun.common.Text;

/**
 * Maven goal which generates graphviz dot and png
 * 
 * @blog http://www.randomnoun.com/wp/2021/06/06/something-yet-be-written/
 */
@Mojo (name = "java-to-graphviz", defaultPhase = LifecyclePhase.PRE_SITE) // GENERATE_SOURCES

public class JavaToGraphvizMojo
    extends AbstractMojo
{
	// FileSet handling from 
	// https://github.com/apache/maven-jar-plugin/blob/maven-jar-plugin-3.2.0/src/main/java/org/apache/maven/plugins/jar/AbstractJarMojo.java

    /**
     * The input files to process.
     */
    @Parameter( property="fileset")
    private FileSet fileset;

    /**
     * Directory containing the generated DOT and PNG files.
     * 
     * <p>If images are being added to generated java documentation, this files in this directory should be copied into the  
     * javadocDirectory of the maven-javadoc-plugin.
     * 
     */
    @Parameter( defaultValue = "${project.build.directory}/java-to-graphviz", required = true )
    private File outputDirectory;

    /**
     * Java source language version.
     * 
     * <p>( 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 8, 9, 10, 11, 12, 13, 14, 15, 16 )
     */
    @Parameter( defaultValue = "11", readonly = true )
    private String source;
    
    /** 
     * Pattern to generate the output filename from the input filename.
     * 
     * <p>Pattern can include:
     * <ul><li>{directory} - the relative directory of the source java file (relative to the <code>fileset</code> base directory)
     * <li>{basename} - the base name of the source java file ( without directory, or extension )
     * <li>{index} - a zero-based diagram index; will only increase if the source file creates multiple diagrams.
     * </ul>
     */
    @Parameter( defaultValue = "{dir}/doc-files/{basename}.png" )
    private String outputFilenamePattern;

    /**
     * Base CSS URL.  
     * Relative to classpath, then file:///./  
     */
    @Parameter( defaultValue = "JavaToGraphviz.css", readonly = true )
    private String baseCssUrl;
    
    /** 
     * URLs to add to the list of user css imports
     */
    @Parameter( property = "userCssUrls" )
    private List<String> userCssUrls;

    /** 
     * Additional CSS rules
     */
    @Parameter( property = "userCssRules" )
    private List<String> userCssRules;

    /** 
     * Option keys and defaults:
     * 
     * <dl><dt>edgerNames=control-flow
     *   <dd>csv list of edger names. Possible edger names: control-flow, ast 
     * <dt>enableKeepNodeFilter=false
     *   <dd>if true, will perform node filtering
     * <dt>defaultKeepNode=true 
     *   <dd>if true, filter will keep nodes by default
     * <dt>keepNode=
     *   <dd>space-separated nodeTypes for fine-grained keepNode control. Prefix nodeType with '-' to exclude, '+' or no prefix to include
     *   <br/> e.g. <code>"-expressionStatement -block -switchCase"</code>
     *   <br/>to exclude those nodeTypes when <code>defaultKeepNode=true</code> 
     *   <p>NB: any node with a 'gv' comment will always be kept
     * </dl>
     */
    @Parameter( property = "options" )
    private Map<String, String> options;

    /**
     * Path to 'dot' executable
     */
    @Parameter( defaultValue = "dot.exe", required = true )
    private File graphvizExecutable;
    
    /**
     * Invoke graphviz 
     */
    @Parameter( property="invokeGraphviz", defaultValue = "true", required = true )
    private boolean invokeGraphviz;
    
    
    
    
    /**
     * True to increase logging
     */
    @Parameter( property="verbose", defaultValue = "false", required = true )
    private boolean verbose;
    
    
    
    /**
     * The {@link {MavenProject}.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    /**
     * The {@link MavenSession}.
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    
    /**
     * @parameter property="settings"
     * @required
     * @since 1.0
     * @readonly
     */
    private Settings settings;


    /**
     *
     */
    @org.apache.maven.plugins.annotations.Component
    private MavenProjectHelper projectHelper;

    
    // private method in FileSetManager

	private DirectoryScanner scan(FileSet fileSet) {
		File basedir = new File(fileSet.getDirectory());
		if (!basedir.exists() || !basedir.isDirectory()) {
			return null;
		}

		DirectoryScanner scanner = new DirectoryScanner();

		String[] includesArray = fileSet.getIncludesArray();
		String[] excludesArray = fileSet.getExcludesArray();

		if (includesArray.length > 0) {
			scanner.setIncludes(includesArray);
		}

		if (excludesArray.length > 0) {
			scanner.setExcludes(excludesArray);
		}

		if (fileSet.isUseDefaultExcludes()) {
			scanner.addDefaultExcludes();
		}

		scanner.setBasedir(basedir);
		scanner.setFollowSymlinks(fileSet.isFollowSymlinks());

		scanner.scan();

		return scanner;
	}
	
	

    
    /**
     * Generates the JAR.
     * 
     * @return The instance of File for the created archive file.
     * @throws MojoExecutionException in case of an error.
     */
	public void createGraphviz() throws MojoExecutionException {
		if (outputDirectory == null) {
			throw new IllegalArgumentException("Missing outputDirectory");
		}
		if (outputFilenamePattern == null) {
			throw new IllegalArgumentException("Missing outputFilename or outputFilenamePattern");
		}
		getLog().info("Creating graphviz diagrams");


		DirectoryScanner scanner = scan(fileset);
		String[] files = scanner.getIncludedFiles(); // also performs exclusion. So way to go, maven.

		try {
		    
		    for (String filename : files) {
		        
		        File dir = new File(filename).getParentFile();
		        //getLog().info("filename " + filename + " path " + dir.getPath());
		        //getLog().info("i " + invokeGraphviz + " path " + graphvizExecutable.getCanonicalPath());
		        
		        File f = new File(fileset.getDirectory(), filename);
		        
                JavaToGraphviz javaToGraphviz = new JavaToGraphviz();
                javaToGraphviz.setFormat("dot");
                javaToGraphviz.setSourceVersion(source);
                if (baseCssUrl != null) {
                    javaToGraphviz.setBaseCssUrl(baseCssUrl);
                }
                javaToGraphviz.setUserCssUrls(userCssUrls);
                javaToGraphviz.setUserCssRules(userCssRules);
                javaToGraphviz.setOptions(options);
              
                getLog().info("Reading " + f.getCanonicalPath());            
                FileInputStream is = new FileInputStream(f);
                javaToGraphviz.parse(is, "UTF-8");
                is.close();
                String n = f.getName();
                if (n.indexOf(".") != -1) { n = n.substring(0, n.indexOf(".")); }
            
                int diagramIndex = 0;
                boolean hasNext;
                do {
                    String on = outputFilenamePattern;

                    on = Text.replaceString(on, "{directory}", dir.getPath());
                    on = Text.replaceString(on, "{basename}", n);
                    on = Text.replaceString(on, "{index}", String.valueOf(diagramIndex));
                    getLog().info("Writing " + on); 
                    File destFile = new File(outputDirectory, on);
                    
                    if (destFile.exists() && destFile.isDirectory()) {
                        throw new MojoExecutionException(destFile + " is a directory.");
                    }
                    if (destFile.exists() && !destFile.canWrite()) {
                        throw new MojoExecutionException(destFile + " is not writable.");
                    }
                    if (!destFile.getParentFile().exists()) {
                        // create the parent directory...
                        if (!destFile.getParentFile().mkdirs()) {
                            // Failure, unable to create specified directory for some unknown reason.
                            throw new MojoExecutionException("Unable to create directory or parent directory of " + destFile);
                        }
                    }
                    if (destFile.exists()) {
                        // delete existing
                        if (!destFile.delete()) {
                            throw new MojoExecutionException("Unable to delete existing file " + destFile);
                        }
                    }
                    
                    
                    FileOutputStream fos = new FileOutputStream(destFile);
                    PrintWriter pw = new PrintWriter(fos);
                    hasNext = javaToGraphviz.writeGraphviz(pw);
                    pw.flush();
                    fos.close();
                    
                    if (invokeGraphviz) {
                        getLog().info("invoking graphviz");
                        if (on.indexOf(".")==-1) {
                            on = on + ".png";
                        } else {
                            on = on.substring(0, on.lastIndexOf(".")) + ".png";
                        }
                        File destPngFile = new File(outputDirectory, on);
                        
                        String output;
                        try {
                            output = ProcessUtil.exec(new String[] {
                                graphvizExecutable.getCanonicalPath(),
                                destFile.getCanonicalPath(),
                                "-Tpng",
                                "-o" + destPngFile.getCanonicalPath()
                            });
                        } catch (ProcessException e) {
                            getLog().info("graphviz process failed", e);
                        }
                    }
                        
                    diagramIndex++;
                } while (hasNext);
		    }
                
		} catch (IOException ioe) {
			// trouble at the mill
			throw new MojoExecutionException("Could not create graphviz diagram", ioe); 
		}

	}
   
    
    public void execute()
        throws MojoExecutionException
    {
        createGraphviz();
    }
    

    
    // getters / setters
    
    /**
     * {@inheritDoc}
     */
    protected Boolean getVerbose() {
     	return verbose;
    }
    
    /**
     * @return the project
     */
    public MavenProject getProject() {
        return project;
    }

    /**
     * @param project
     * the project to set
     */
    public void setProject(final MavenProject project) {
        this.project = project;
    }

    /**
     * @return the settings
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * @param settings
     * the settings to set
     */
    public void setSettings(final Settings settings) {
        this.settings = settings;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getOutputFilenamePattern() {
        return outputFilenamePattern;
    }

    public void setOutputFilenamePattern(String outputFilenamePattern) {
        this.outputFilenamePattern = outputFilenamePattern;
    }

    public List<String> getUserCssUrls() {
        return userCssUrls;
    }

    public void setUserCssUrls(List<String> userCssUrls) {
        this.userCssUrls = userCssUrls;
    }

    public List<String> getUserCssRules() {
        return userCssRules;
    }

    public void setUserCssRules(List<String> userCssRules) {
        this.userCssRules = userCssRules;
    }

    public File getGraphvizExecutable() {
        return graphvizExecutable;
    }

    public void setGraphvizExecutable(File graphvizExecutable) {
        this.graphvizExecutable = graphvizExecutable;
    }

    public boolean isInvokeGraphviz() {
        return invokeGraphviz;
    }

    public void setInvokeGraphviz(boolean invokeGraphviz) {
        this.invokeGraphviz = invokeGraphviz;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }




    public String getSource() {
        return source;
    }




    public void setSource(String source) {
        this.source = source;
    }




    public Map<String, String> getOptions() {
        return options;
    }




    public void setOptions(Map<String, String> options) {
        this.options = options;
    }
    
}
