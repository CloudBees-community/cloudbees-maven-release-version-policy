/*
 * Copyright 2015,CloudBees Inc.
 *
 * Licensed under the Apache License,Version2.0(the"License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,software
 * distributed under the License is distributed on an"AS IS"BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudbees.maven.release.policy.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.shared.release.policy.PolicyException;
import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.policy.version.VersionPolicyResult;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.codehaus.plexus.component.annotations.Component;

/**
 * A VersionPolicy implementation that uses the CloudBees conventions for updating version numbers.
 *
 * @since 1.0
 */
@Component(role = VersionPolicy.class,
           hint = "cloudbees",
           description = "A VersionPolicy implementation that uses the CloudBees conventions for updating version "
                   + "numbers")
public class CloudBeesVersionPolicy implements VersionPolicy {
    /**
     * {@inheritDoc}
     */
    public VersionPolicyResult getReleaseVersion(VersionPolicyRequest versionPolicyRequest)
            throws PolicyException, VersionParseException {
        // this one is easy, just always throw away the -SNAPSHOT
        return new VersionPolicyResult()
                .setVersion(StringUtils.removeEnd(versionPolicyRequest.getVersion(), "-SNAPSHOT"));
    }

    /**
     * {@inheritDoc}
     */
    public VersionPolicyResult getDevelopmentVersion(VersionPolicyRequest versionPolicyRequest)
            throws PolicyException, VersionParseException {
        // this one is harder, we basically want to find the last incrementally updatable segment and bump it
        final VersionPolicyResult result = new VersionPolicyResult();
        String version = versionPolicyRequest.getVersion();
        if (version.endsWith("-SNAPSHOT")) {
            // if it is a -SNAPSHOT already, we shouldn't have been called, so we leave the value as is
            result.setVersion(version);
        } else {
            // find the first version format string (using "-" as separator)
            String [] comps = version.split("-");
            String externalRef = "";
            if (comps.length > 1 
                    && !(version.contains("alpha") || version.contains("ALPHA") || version.contains("beta") || version.contains("BETA"))) {
                version = comps[0];
                for (int i = 1; i < comps.length; i++) {
                    externalRef += "-" + comps[i];
                }
            }
            // find the last numeric segment and increment that
            Pattern separator = Pattern.compile("([\\.-])");
            Matcher m = separator.matcher(version);
            List<Integer> offsets = new ArrayList<Integer>();
            offsets.add(0);
            while (m.find()) {
                if (m.start() > 0) {
                    offsets.add(m.start());
                    offsets.add(m.start() + 1);
                }
            }
            offsets.add(version.length());
            boolean matched = false;
            for (int i = offsets.size() - 2; i >= 0; i -= 2) {
                final String segment = version.substring(offsets.get(i), offsets.get(i + 1));
                if (segment.matches("\\d+")) {
                    result.setVersion(version.substring(0, offsets.get(i))
                            + new BigInteger(segment).add(BigInteger.ONE).toString()
                            + version.substring(offsets.get(i + 1))
                            + externalRef
                            + "-SNAPSHOT");
                    matched = true;
                    break;
                }
                // we will handle -ALPHA -> BETA-1 and .ALPHA -> .BETA.1 (note we add a segment)
                if ("alpha".equals(segment)) {
                    // from the regex it is either a '.' or a '-' so charAt is safe from multi-word code points
                    char separatorChar = (offsets.get(i) > 0 ? version.charAt(offsets.get(i) - 1) : '-');
                    result.setVersion(version.substring(0, offsets.get(i))
                            + "beta" + separatorChar + "1"
                            + version.substring(offsets.get(i + 1))
                            + "-SNAPSHOT");
                    matched = true;
                    break;
                }
                if ("ALPHA".equals(segment)) {
                    // from the regex it is either a '.' or a '-' so charAt is safe from multi-word code points
                    char separatorChar = (offsets.get(i) > 0 ? version.charAt(offsets.get(i) - 1) : '-');
                    result.setVersion(version.substring(0, offsets.get(i))
                            + "BETA" + separatorChar + "1"
                            + version.substring(offsets.get(i + 1))
                            + "-SNAPSHOT");
                    matched = true;
                    break;
                }
                if ("beta".equals(segment)) {
                    // from the regex it is either a '.' or a '-' so charAt is safe from multi-word code points
                    char separatorChar = (offsets.get(i) > 0 ? version.charAt(offsets.get(i) - 1) : '-');
                    result.setVersion(version.substring(0, offsets.get(i))
                            + "rc" + separatorChar + "1"
                            + version.substring(offsets.get(i + 1))
                            + "-SNAPSHOT");
                    matched = true;
                    break;
                }
                if ("BETA".equals(segment)) {
                    // from the regex it is either a '.' or a '-' so charAt is safe from multi-word code points
                    char separatorChar = (offsets.get(i) > 0 ? version.charAt(offsets.get(i) - 1) : '-');
                    result.setVersion(version.substring(0, offsets.get(i))
                            + "RC" + separatorChar + "1"
                            + version.substring(offsets.get(i + 1))
                            + "-SNAPSHOT");
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new VersionParseException("Unsupported version number format: " + version);
            }
        }
        return result;
    }
}
