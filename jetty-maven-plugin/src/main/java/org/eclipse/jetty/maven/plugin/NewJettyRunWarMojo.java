//
//  ========================================================================
//  Copyright (c) 1995-2019 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//


package org.eclipse.jetty.maven.plugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.jetty.util.PathWatcher;
import org.eclipse.jetty.util.PathWatcher.PathWatchEvent;
import org.eclipse.jetty.util.StringUtil;

/**
* <p>
*  This goal is used to assemble your webapp into a war and automatically deploy it to Jetty.
*  </p>
*  <p>
*  Once invoked, the plugin runs continuously and can be configured to scan for changes in the project and to the
*  war file and automatically perform a hot redeploy when necessary. 
*  </p>
*  <p>
*  You may also specify the location of a jetty.xml file whose contents will be applied before any plugin configuration.
*  </p>
*  Runs jetty on a war file
*/
@Mojo( name = "newrun-war", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class NewJettyRunWarMojo extends AbstractWebAppMojo
{   
    //Start of parameters only valid for runType=EMBED  
    /**
     * The interval in seconds to pause before checking if changes
     * have occurred and re-deploying as necessary. A value 
     * of 0 indicates no re-deployment will be done. In that case, you
     * can force redeployment by typing a linefeed character at the command line.
     */
    @Parameter(defaultValue="0", property="jetty.scan", required=true)
    protected int scan; 
    
    /**
     * Scanner to check for files changes to cause redeploy
     */
    protected PathWatcher scanner;
    protected JettyEmbedder embedder;
    protected JettyForker forker;
    protected JettyDistroForker distroForker;
    protected Path war;
    
    
    /**
     * Do not re-process the war overlays, this will already
     * have been done by the build phase of this mojo.
     */
    @Override
    public List<Overlay> getOverlays() throws Exception
    {
       return null;
    }
    
    @Override
    public void configureWebApp() throws Exception
    {
        super.configureWebApp();
        if (StringUtil.isBlank(webApp.getWar()))
        {
            war = target.toPath().resolve(project.getBuild().getFinalName()+".war");
            webApp.setWar(war.toFile().getAbsolutePath());
        }
        else
            war = Paths.get(webApp.getWar());
        
        getLog().info("War = "+war);
    }

    /**
     * Start a jetty instance in process to run the built war.
     */
    @Override
    public void startJettyEmbedded() throws MojoExecutionException
    {
        try
        {
            embedder = newJettyEmbedder();        
            embedder.setExitVm(true);
            embedder.setStopAtShutdown(true);
            embedder.start();
            startScanner();
            embedder.join();
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Error starting jetty", e);
        }
    }

    
    /**
     * Fork a jetty instance to run the built war.
     */
    @Override
    public void startJettyForked() throws MojoExecutionException
    {
        try
        {
            forker = newJettyForker();
            forker.setWaitForChild(true); //we run at the command line, echo child output and wait for it
            startScanner();
            forker.start(); //forks jetty instance
            
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Error starting jetty", e);
        }
    }

    /**
     * Deploy the built war to a jetty distro.
     */
    @Override
    public void startJettyDistro() throws MojoExecutionException
    {
        try
        {
            distroForker = newJettyDistroForker();
            distroForker.setWaitForChild(true); //we always run at the command line, echo child output and wait for it
            startScanner();
            distroForker.start(); //forks a jetty distro

        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Error starting jetty", e);
        }
    }
    
    public void startScanner()
    throws Exception
    {
        // start scanning for changes, or wait for linefeed on stdin
        if (scan > 0)
        {
            scanner = new PathWatcher();
            configureScanner ();
            scanner.setNotifyExistingOnStart(false);
            scanner.start();
        }
        else
        {
            ConsoleReader creader = new ConsoleReader();
            creader.addListener(new ConsoleReader.Listener()
            {
                @Override
                public void consoleEvent(String line)
                {
                    try
                    {
                        restartWebApp(false);
                    }
                    catch (Exception e)
                    {
                        getLog().debug(e);
                    }
                }
            });
            Thread cthread = new Thread(creader, "ConsoleReader");
            cthread.setDaemon(true);
            cthread.start();
        }
    }

    public void configureScanner() throws MojoExecutionException
    {
        scanner.watch(project.getFile().toPath());
        scanner.watch(war);

        scanner.addListener(new PathWatcher.EventListListener()
        {
            @Override
            public void onPathWatchEvents(List<PathWatchEvent> events)
            {
                try
                {
                    boolean reconfigure = false;
                    for (PathWatchEvent e:events)
                    {
                        if (e.getPath().equals(project.getFile().toPath()))
                        {
                            reconfigure = true;
                            break;
                        }
                    }
                    restartWebApp(reconfigure);
                }
                catch (Exception e)
                {
                    getLog().error("Error reconfiguring/restarting webapp after change in watched files",e);
                }
            }
        });
    }

    public void restartWebApp(boolean reconfigure) throws Exception 
    {
        getLog().info("Restarting webapp ...");
        getLog().debug("Stopping scanner ...");
        if (scanner != null)
            scanner.stop();
        
        switch (runType)
        {
            case EMBED:
            {
                getLog().debug("Reconfiguring webapp ...");
                
                verifyPomConfiguration();
                // check if we need to reconfigure the scanner,
                // which is if the pom changes
                if (reconfigure)
                {
                    getLog().info("Reconfiguring scanner after change to pom.xml ...");
                    scanner.reset();
                    warArtifacts = null;
                    configureScanner();
                }
                embedder.getWebApp().stop();
                configureWebApp();
                embedder.redeployWebApp();
                scanner.start();
                getLog().info("Restart completed at "+new Date().toString());
                
                break;
            }
            case FORK:
            {
                verifyPomConfiguration();
                if (reconfigure)
                {
                    getLog().info("Reconfiguring scanner after change to pom.xml ...");
                    scanner.reset();
                    warArtifacts = null; ///TODO if the pom changes for the forked case, how would we get the forked process to stop and restart?
                    configureScanner();
                }
                
                configureWebApp();
                //regenerate with new config and restart the webapp
                forker.redeployWebApp();
                //restart scanner
                scanner.start();
                
                break;
            }
            case DISTRO:
            {
                verifyPomConfiguration();
                if (reconfigure)
                {
                    getLog().info("Reconfiguring scanner after change to pom.xml ...");
                    scanner.reset();
                    warArtifacts = null; //TODO if there are any changes to the pom, then we would have to tell the
                    //existing forked distro process to stop, then rerun the configuration and then refork - too complicated??!
                    configureScanner();
                }
                configureWebApp();
                //regenerate the webapp and redeploy it
                distroForker.redeployWebApp();
                //restart scanner
                scanner.start();

                break;
            }
            default:
            {
                throw new IllegalStateException("Unrecognized run type "+runType);
            }
        }
        getLog().info("Restart completed.");
    }
}
