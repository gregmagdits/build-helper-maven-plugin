package org.codehaus.mojo.buildhelper;

/*
 * The MIT License
 *
 * Copyright (c) 2004, The Codehaus
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

import java.util.List;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Resolve the latest released version of this project. This mojo sets the
 * following properties:
 *
 * <pre>
 *   [propertyPrefix].version
 *   [propertyPrefix].majorVersion
 *   [propertyPrefix].minorVersion
 *   [propertyPrefix].incrementalVersion
 * </pre>
 *
 * Where the propertyPrefix is the string set in the mojo parameter.
 *
 * @author Robert Scholte
 * @since 1.6
 */
@Mojo(name = "latest-version", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class LatestVersionMojo extends AbstractDefinePropertyMojo {

    /**
     * The artifact metadata source to use.
     */
    @Component
    private ArtifactMetadataSource artifactMetadataSource;

    @Component
    private ArtifactFactory artifactFactory;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    private ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true)
    private List<ArtifactRepository> remoteArtifactRepositories;

    /**
     * Prefix string to use for the set of version properties.
     */
    @Parameter(defaultValue = "latestVersion")
    private String propertyPrefix;

    private void defineVersionProperty(String name, String value) {
        defineProperty(propertyPrefix + '.' + name, value);
    }

    private void defineVersionProperty(String name, int value) {
        defineVersionProperty(name, Integer.toString(value));
    }

    @SuppressWarnings("unchecked")
    public void execute() {
        org.apache.maven.artifact.Artifact artifact = artifactFactory.createArtifact(getProject().getGroupId(), getProject().getArtifactId(), "", "", "");
        try {
            ArtifactVersion version = null;
            List<ArtifactVersion> versions = artifactMetadataSource.retrieveAvailableVersions(artifact, localRepository, remoteArtifactRepositories);
            for (ArtifactVersion v : versions) {
                if ((version == null || v.compareTo(version) > 0)) {
                    version = v;
                }
            }

            if (version != null) {
                // Use ArtifactVersion.toString(), the major, minor and incrementalVersion
                // return all an int.
                String versionValue = version.toString();

                // This would not always reflect the expected version.
                int dashIndex = versionValue.indexOf('-');
                if (dashIndex >= 0) {
                    versionValue = versionValue.substring(0, dashIndex);
                }
                getLog().info("Latest version determined to be " + versionValue);
                defineVersionProperty("version", versionValue);
                defineVersionProperty("majorVersion", version.getMajorVersion());
                defineVersionProperty("minorVersion", version.getMinorVersion());
                defineVersionProperty("incrementalVersion", version.getIncrementalVersion());
                defineVersionProperty("buildNumber", version.getBuildNumber());
                defineVersionProperty("qualifier", version.getQualifier());
            }

        } catch (ArtifactMetadataRetrievalException e) {
            if (getLog().isWarnEnabled()) {
                getLog().warn("Failed to retrieve artifacts metadata, cannot resolve the released version");
            }
        }
    }
}
