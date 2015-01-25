/*********************************************
 * Copyright (c) 2013 LeanPulse.
 * All rights reserved.
 *********************************************/
package com.leanpulse.bibant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.DemuxInputStream;
import org.apache.tools.ant.DemuxOutputStream;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.ProjectHelperRepository;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskAdapter;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.listener.Log4jListener;
import org.apache.tools.ant.listener.SimpleBigProjectLogger;
import org.apache.tools.ant.property.ResolvePropertyMap;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.ProxySetup;


/**
 * Command line entry point into Ant. This class is entered via the
 * canonical `public static void main` entry point and reads the
 * command line arguments. It then assembles and executes an Ant
 * project.
 * <p>
 * If you integrating Ant into some other tool, this is not the class
 * to use as an entry point. Please see the source code of this
 * class to see how it manipulates the Ant project classes.
 *
 */
public class Launcher {
	
	public static final String BIBANT_VER = /*@BIBANTVER@*/"1.6"/*@BIBANTVER@*/;
	public static final String BIBANT_DATE = /*@BIBANTDATE@*/"20-October-2014"/*@BIBANTDATE@*/;

    /** Our current message output status. Follows Project.MSG_XXX. */
    private int msgOutputLevel = Project.MSG_INFO;

    /** File that we are using for configuration. */
    private File buildFile; /* null */

    /** Stream to use for logging. */
    private static PrintStream out = System.out;

    /** Stream that we are using for logging error messages. */
    private static PrintStream err = System.err;

    /** The build targets. */
    private Vector<String> targets = new Vector<String>();

    /** Set of properties that can be used by tasks. */
    private Properties definedProps = new Properties();

    /** File names of property files to load on startup. */
    private Vector<String> propertyFiles = new Vector<String>(1);

    /** keep going mode */
    private boolean keepGoingMode = false;

    /**
     * Whether or not this instance has successfully been
     * constructed and is ready to run.
     */
    private boolean readyToRun = false;

    /**
     * Whether or not a logfile is being used. This is used to
     * check if the output streams must be closed.
     */
    private static boolean isLogFileUsed = false;

    /**
     * proxy flag: default is false
     */
    private boolean proxy = false;

    /**
     * Prints the message of the Throwable if it (the message) is not
     * <code>null</code>.
     *
     * @param t Throwable to print the message of.
     *          Must not be <code>null</code>.
     */
    private static void printMessage(Throwable t) {
        String message = t.getMessage();
        if (message != null) {
            System.err.println(message);
        }
    }

    /**
     * Start BibAnt
     * @param args command line args
     */
    public void startBibAnt(String[] args) {
    	String lSep = System.getProperty("line.separator");
        StringBuffer msg = new StringBuffer();
        msg.append("*************************************************************" + lSep);
        msg.append("* BibAnt v" + BIBANT_VER + " - The Documentation Packages Builder" + lSep);
        msg.append("* Copyright " + Calendar.getInstance().get(Calendar.YEAR) + " LeanPulse (http://www.leanpulse.com/)" + lSep);
        msg.append("*************************************************************" + lSep);
        System.out.println(msg);
        
        try {
            processArgs(args);
        } catch (Throwable exc) {
            handleLogfile();
            printMessage(exc);
            exit(1);
            return;
        }

        // expect the worst
        int exitCode = 1;
        try {
            try {
                runBuild();
                exitCode = 0;
            } catch (ExitStatusException ese) {
                exitCode = ese.getStatus();
                if (exitCode != 0) {
                    throw ese;
                }
            }
        } catch (BuildException be) {
            if (err != System.err) {
                printMessage(be);
            }
        } catch (Throwable exc) {
            exc.printStackTrace();
            printMessage(exc);
        } finally {
            handleLogfile();
        }
        exit(exitCode);
    }

    /**
     * This operation is expected to call {@link System#exit(int)}, which
     * is what the base version does.
     * However, it is possible to do something else.
     * @param exitCode code to exit with
     */
    protected void exit(int exitCode) {
        System.exit(exitCode);
    }

    /**
     * Close logfiles, if we have been writing to them.
     *
     * @since Ant 1.6
     */
    private static void handleLogfile() {
        if (isLogFileUsed) {
            FileUtils.close(out);
            FileUtils.close(err);
        }
    }

    /**
     * Command line entry point. This method kicks off the building
     * of a project object and executes a build using either a given
     * target or the default target.
     *
     * @param args Command line arguments. Must not be <code>null</code>.
     */
    public static void main(String[] args) {
    	Launcher main = new Launcher();
        main.startBibAnt(args);
    }

    /**
     * Constructor used when creating Main for later arg processing
     * and startup
     */
    public Launcher() {
    }

    /**
     * Process command line arguments.
     * When ant is started from Launcher, launcher-only arguments do not get
     * passed through to this routine.
     *
     * @param args the command line arguments.
     *
     * @since Ant 1.6
     */
    private void processArgs(String[] args) {
        PrintStream logTo = null;

        // cycle through given args

        boolean justPrintVersion = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("-version")) {
                justPrintVersion = true;
            } else if (arg.equals("-quiet") || arg.equals("-q")) {
                msgOutputLevel = Project.MSG_WARN;
            } else if (arg.equals("-verbose") || arg.equals("-v")) {
                msgOutputLevel = Project.MSG_VERBOSE;
            } else if (arg.equals("-debug") || arg.equals("-d")) {
                msgOutputLevel = Project.MSG_DEBUG;
            } else if (arg.equals("-logfile") || arg.equals("-l")) {
                try {
                    File logFile = new File(args[i + 1]);
                    i++;
                    logTo = new PrintStream(new FileOutputStream(logFile));
                    isLogFileUsed = true;
                } catch (IOException ioe) {
                    String msg = "Cannot write on the specified log file. "
                        + "Make sure the path exists and you have write "
                        + "permissions.";
                    throw new BuildException(msg);
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    String msg = "You must specify a log file when "
                        + "using the -log argument";
                    throw new BuildException(msg);
                }
            } else if (arg.equals("-buildfile") || arg.equals("-file")
                       || arg.equals("-f")) {
                i = handleArgBuildFile(args, i);
            } else if (arg.startsWith("-D")) {
                i = handleArgDefine(args, i);
            } else if (arg.startsWith("-propertyfile")) {
                i = handleArgPropertyFile(args, i);
            } else if (arg.equals("-k") || arg.equals("-keep-going")) {
                keepGoingMode = true;
            } else if (arg.equals("-autoproxy")) {
                proxy = true;
            } else if (arg.startsWith("-")) {
                // we don't have any more args to recognize!
                String msg = "Unknown argument: " + arg;
                System.err.println(msg);
                printUsage();
                throw new BuildException("");
            } else {
                // if it's no other arg, it may be the target
                targets.addElement(arg);
            }
        }

        if (msgOutputLevel >= Project.MSG_VERBOSE || justPrintVersion) {
            printVersion(msgOutputLevel);
        }

        if (justPrintVersion)
        	return;
        
        if (buildFile == null) {
            // no build file specified: so search an existing default file
            Iterator<?> it = ProjectHelperRepository.getInstance().getHelpers();
            do {
                ProjectHelper helper = (ProjectHelper) it.next();
                buildFile = new File(helper.getDefaultBuildFile());
                if (msgOutputLevel >= Project.MSG_VERBOSE) {
                    System.out.println("Trying the default build file: " + buildFile);
                }
            } while (!buildFile.exists() && it.hasNext());
        }

        // make sure buildfile exists
        if (!buildFile.exists()) {
            System.out.println("Buildfile: " + buildFile + " does not exist!");
            throw new BuildException("Build failed");
        }

        if (buildFile.isDirectory()) {
            File whatYouMeant = new File(buildFile, "build.xml");
            if (whatYouMeant.isFile()) {
                buildFile = whatYouMeant;
            } else {
                System.out.println("What? Buildfile: " + buildFile + " is a dir!");
                throw new BuildException("Build failed");
            }
        }

        // Normalize buildFile for re-import detection
        buildFile = FileUtils.getFileUtils().normalize(buildFile.getAbsolutePath());

        // Load the property files specified by -propertyfile
        loadPropertyFiles();

        if (msgOutputLevel >= Project.MSG_INFO) {
            System.out.println("Buildfile: " + buildFile);
        }

        if (logTo != null) {
            out = logTo;
            err = logTo;
            System.setOut(out);
            System.setErr(err);
        }
        readyToRun = true;
    }

    // --------------------------------------------------------
    //    Methods for handling the command line arguments
    // --------------------------------------------------------

    /** Handle the -buildfile, -file, -f argument */
    private int handleArgBuildFile(String[] args, int pos) {
        try {
            buildFile = new File(
                args[++pos].replace('/', File.separatorChar));
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new BuildException(
                "You must specify a buildfile when using the -buildfile argument");
        }
        return pos;
    }

    /** Handler -D argument */
    private int handleArgDefine(String[] args, int argPos) {
        /* Interestingly enough, we get to here when a user
         * uses -Dname=value. However, in some cases, the OS
         * goes ahead and parses this out to args
         *   {"-Dname", "value"}
         * so instead of parsing on "=", we just make the "-D"
         * characters go away and skip one argument forward.
         *
         * I don't know how to predict when the JDK is going
         * to help or not, so we simply look for the equals sign.
         */
        String arg = args[argPos];
        String name = arg.substring(2, arg.length());
        String value = null;
        int posEq = name.indexOf("=");
        if (posEq > 0) {
            value = name.substring(posEq + 1);
            name = name.substring(0, posEq);
        } else if (argPos < args.length - 1) {
            value = args[++argPos];
        } else {
            throw new BuildException("Missing value for property "
                                     + name);
        }
        definedProps.put(name, value);
        return argPos;
    }

    /** Handle the -propertyfile argument. */
    private int handleArgPropertyFile(String[] args, int pos) {
        try {
            propertyFiles.addElement(args[++pos]);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            String msg = "You must specify a property filename when "
                + "using the -propertyfile argument";
            throw new BuildException(msg);
        }
        return pos;
    }

    // --------------------------------------------------------
    //    other methods
    // --------------------------------------------------------

    /** Load the property files specified by -propertyfile */
    private void loadPropertyFiles() {
        for (int propertyFileIndex = 0;
             propertyFileIndex < propertyFiles.size();
             propertyFileIndex++) {
            String filename = propertyFiles.elementAt(propertyFileIndex);
            Properties props = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(filename);
                props.load(fis);
            } catch (IOException e) {
                System.out.println("Could not load property file "
                                   + filename + ": " + e.getMessage());
            } finally {
                FileUtils.close(fis);
            }

            // ensure that -D properties take precedence
            Enumeration<?> propertyNames = props.propertyNames();
            while (propertyNames.hasMoreElements()) {
                String name = (String) propertyNames.nextElement();
                if (definedProps.getProperty(name) == null) {
                    definedProps.put(name, props.getProperty(name));
                }
            }
        }
    }

    /**
     * Executes the build. If the constructor for this instance failed
     * (e.g. returned after issuing a warning), this method returns
     * immediately.
     *
     * @exception BuildException if the build fails
     */
    private void runBuild() throws BuildException {

        if (!readyToRun) {
            return;
        }

        final Project project = new Project();

        Throwable error = null;

        try {
        	BuildLogger logger = new SimpleBigProjectLogger();
            logger.setMessageOutputLevel(msgOutputLevel);
            logger.setOutputPrintStream(out);
            logger.setErrorPrintStream(err);
            project.addBuildListener(logger);
            
            BuildListener listener = new Log4jListener();
            project.setProjectReference(listener);
            project.addBuildListener(listener);
            
            project.setInputHandler(new DefaultInputHandler());

            PrintStream savedErr = System.err;
            PrintStream savedOut = System.out;
            InputStream savedIn = System.in;

            // use a system manager that prevents from System.exit()
            SecurityManager oldsm = null;
            oldsm = System.getSecurityManager();

            //SecurityManager can not be installed here for backwards
            //compatibility reasons (PD). Needs to be loaded prior to
            //ant class if we are going to implement it.
            //System.setSecurityManager(new NoExitSecurityManager());
            try {
                project.setDefaultInputStream(System.in);
                System.setIn(new DemuxInputStream(project));
                System.setOut(new PrintStream(new DemuxOutputStream(project, false)));
                System.setErr(new PrintStream(new DemuxOutputStream(project, true)));
                
                project.fireBuildStarted();

                project.init();

                // init additional tasks
                ComponentHelper componentHelper = ComponentHelper.getComponentHelper(project);
                initTasksFromPropsFile(componentHelper, "/com/leanpulse/bibant/taskdefs/taskdefs.properties", "default BibAnt task list");
                initTypesFromPropsFile(componentHelper, "/com/leanpulse/bibant/taskdefs/typedefs.properties", "default BibAnt types list");
                initTasksFromPropsFile(componentHelper, "/net/sf/antcontrib/antcontrib.properties", "default AntContrib task list");
                componentHelper.addTaskDefinition("for", net.sf.antcontrib.logic.ForTask.class);
                componentHelper.addTaskDefinition("xmltask", com.oopsconsultancy.xmltask.ant.XmlTask.class);

                // resolve properties
                PropertyHelper propertyHelper
                    = (PropertyHelper) PropertyHelper.getPropertyHelper(project);
                HashMap<Object,Object> props = new HashMap<Object,Object>(definedProps);
                new ResolvePropertyMap(project, propertyHelper,
                                       propertyHelper.getExpanders())
                    .resolveAllProperties(props, null, false);

                // set user-define properties
                for(Iterator<Map.Entry<Object,Object>> e = props.entrySet().iterator(); e.hasNext(); ) {
                    Map.Entry<Object,Object> ent = e.next();
                    String arg = (String) ent.getKey();
                    Object value = ent.getValue();
                    project.setUserProperty(arg, String.valueOf(value));
                }

                project.setUserProperty(MagicNames.ANT_FILE,
                                        buildFile.getAbsolutePath());
                project.setUserProperty(MagicNames.ANT_FILE_TYPE,
                                        MagicNames.ANT_FILE_TYPE_FILE);

                project.setKeepGoingMode(keepGoingMode);
                if (proxy) {
                    //proxy setup if enabled
                    ProxySetup proxySetup = new ProxySetup(project);
                    proxySetup.enableProxies();
                }

                ProjectHelper.configureProject(project, buildFile);

                // make sure that we have a target to execute
                if (targets.size() == 0) {
                    if (project.getDefaultTarget() != null) {
                        targets.addElement(project.getDefaultTarget());
                    }
                }

                project.executeTargets(targets);
            } finally {
                // put back the original security manager
                //The following will never eval to true. (PD)
                if (oldsm != null) {
                    System.setSecurityManager(oldsm);
                }

                System.setOut(savedOut);
                System.setErr(savedErr);
                System.setIn(savedIn);
            }
        } catch (RuntimeException exc) {
            error = exc;
            throw exc;
        } catch (Error e) {
            error = e;
            throw e;
        } finally {
            try {
                project.fireBuildFinished(error);
            } catch (Throwable t) {
                // yes, I know it is bad style to catch Throwable,
                // but if we don't, we lose valuable information
                System.err.println("Caught an exception while logging the"
                                   + " end of the build.  Exception was:");
                t.printStackTrace();
                if (error != null) {
                    System.err.println("There has been an error prior to"
                                       + " that:");
                    error.printStackTrace();
                }
                throw new BuildException(t);
            }
        }
    }
    
    private void initTasksFromPropsFile(ComponentHelper componentHelper, String propsFile, String fileDes) {
    	Properties taskdefs = null;
        InputStream in = null;
        try {
            in = Launcher.class.getResourceAsStream(propsFile);
            if (in == null) {
                throw new BuildException("Can't load " + fileDes);
            }
            taskdefs = new Properties();
            taskdefs.load(in);
        } catch (IOException e) {
            throw new BuildException("Can't load " + fileDes, e);
        } finally {
            FileUtils.close(in);
        }
        Enumeration<?> e = taskdefs.propertyNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String className = taskdefs.getProperty(name);
            AntTypeDefinition def = new AntTypeDefinition();
            def.setName(name);
            def.setClassLoader(ClassLoader.getSystemClassLoader());
            def.setAdapterClass(TaskAdapter.class);
            def.setClassName(className);
            def.setAdaptToClass(Task.class);
            componentHelper.addDataTypeDefinition(def);
        }
    }
    
    private void initTypesFromPropsFile(ComponentHelper componentHelper, String propsFile, String fileDes) {
    	Properties typedefs = null;
        InputStream in = null;
        try {
            in = Launcher.class.getResourceAsStream(propsFile);
            if (in == null) {
                throw new BuildException("Can't load " + fileDes);
            }
            typedefs = new Properties();
            typedefs.load(in);
        } catch (IOException e) {
            throw new BuildException("Can't load " + fileDes, e);
        } finally {
            FileUtils.close(in);
        }
        Enumeration<?> e = typedefs.propertyNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String className = typedefs.getProperty(name);
            AntTypeDefinition def = new AntTypeDefinition();
            def.setName(name);
            def.setClassName(className);
            def.setClassLoader(ClassLoader.getSystemClassLoader());
            componentHelper.addDataTypeDefinition(def);
        }
    }

    /**
     * Prints the usage information for this class to <code>System.out</code>.
     */
    private static void printUsage() {
        String lSep = System.getProperty("line.separator");
        StringBuffer msg = new StringBuffer();
        msg.append("ant [options] [target [target2 [target3] ...]]" + lSep);
        msg.append("Options: " + lSep);
        msg.append("  -buildfile <file>      use given buildfile" + lSep);
        msg.append("    -file    <file>              ''" + lSep);
        msg.append("    -f       <file>              ''" + lSep);
        msg.append("  -D<property>=<value>   use value for given property" + lSep);
        msg.append("  -propertyfile <name>   load all properties from file with -D" + lSep);
        msg.append("                         properties taking precedence" + lSep);
        msg.append("  -quiet, -q             be extra quiet" + lSep);
        msg.append("  -verbose, -v           be extra verbose" + lSep);
        msg.append("  -debug, -d             print debugging information" + lSep);
        msg.append("  -logfile <file>        use given file for log" + lSep);
        msg.append("    -l     <file>                ''" + lSep);
        msg.append("  -keep-going, -k        execute all targets that do not depend" + lSep);
        msg.append("                         on failed target(s)" + lSep);
        msg.append("  -autoproxy             use the OS proxy settings" + lSep);
        msg.append("  -version               print the version information and exit" + lSep);
        msg.append("  -help, -h              print this message" + lSep);
        System.out.println(msg.toString());
    }

    /**
     * Prints the Ant version information to <code>System.out</code>.
     *
     * @exception BuildException if the version information is unavailable
     */
    private static void printVersion(int logLevel) throws BuildException {
        System.out.println(getBibAntVersion());
    }

    /**
     * Cache of the Ant version information when it has been loaded.
     */
    private static String bibantVersion = null;

    /**
     * Returns the BibAnt version information, if available. Once the information
     * has been loaded once, it's cached and returned from the cache on future
     * calls.
     *
     * @return the Ant version information as a String
     *         (always non-<code>null</code>)
     *
     * @exception BuildException if the version information is unavailable
     */
    public static synchronized String getBibAntVersion() throws BuildException {
        if (bibantVersion == null) {
            StringBuffer msg = new StringBuffer();
            msg.append("LeanPulse BibAnt v");
            msg.append(BIBANT_VER);
            msg.append(" compiled on ");
            msg.append(BIBANT_DATE);
            bibantVersion = msg.toString();
        }
        return bibantVersion;
    }
    
    
}
