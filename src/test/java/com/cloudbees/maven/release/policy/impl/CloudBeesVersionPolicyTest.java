package com.cloudbees.maven.release.policy.impl;

import org.apache.maven.shared.release.policy.PolicyException;
import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicyRequest;
import org.apache.maven.shared.release.versions.VersionParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class CloudBeesVersionPolicyTest {
    private VersionPolicy instance;

    @Before
    public void setUp() {
        instance = new CloudBeesVersionPolicy();
    }

    @After
    public void tearDown() {
        instance = null;
    }

    private static VersionPolicyRequest request(String version) {
        return new VersionPolicyRequest().setVersion(version);
    }


    @Test
    public void singleDigitVersions() throws Exception {
        assertThat(instance.getDevelopmentVersion(request("1")).getVersion(), is("2-SNAPSHOT"));
    }

    @Test
    public void doubleDigitVersions() throws Exception {
        assertThat(instance.getDevelopmentVersion(request("1.2")).getVersion(), is("1.3-SNAPSHOT"));
    }

    @Test
    public void trippleDigitVersions() throws Exception {
        assertThat(instance.getDevelopmentVersion(request("1.2.3")).getVersion(), is("1.2.4-SNAPSHOT"));
    }

    @Test
    public void quadDigitVersions() throws Exception {
        assertThat(instance.getDevelopmentVersion(request("1.2.3.4")).getVersion(), is("1.2.3.5-SNAPSHOT"));
    }

    @Test
    public void mixedDigitVersions() throws Exception {
        assertThat(instance.getDevelopmentVersion(request("1.2-3.4")).getVersion(), is("1.2-3.5-SNAPSHOT"));
    }

    @Test(expected = VersionParseException.class)
    public void emptyVersion() throws Exception {
        instance.getDevelopmentVersion(request("")).getVersion();
    }

    @Test(expected = VersionParseException.class)
    public void textVersion() throws Exception {
        instance.getDevelopmentVersion(request("trunk")).getVersion();
    }

    @Theory
    public void testData(Data data) throws VersionParseException, PolicyException {
        assertThat(instance.getReleaseVersion(request(data.before)).getVersion(), is(data.release));
        assertThat(instance.getDevelopmentVersion(request(data.release)).getVersion(), is(data.after));
    }

    @DataPoints
    public static Data[] data() {
        return new Data[]{
                d("1.8.0-SNAPSHOT", "1.8.0", "1.8.1-SNAPSHOT"),
                d("1.8.0.-SNAPSHOT", "1.8.0.", "1.8.1.-SNAPSHOT"),
                d("1.8.0-superstars-SNAPSHOT", "1.8.0-superstars", "1.8.1-superstars-SNAPSHOT"),
                d("1.8.9-SNAPSHOT", "1.8.9", "1.8.10-SNAPSHOT"),
                d("1.8.10-SNAPSHOT", "1.8.10", "1.8.11-SNAPSHOT"),
                d("1.8.99-SNAPSHOT", "1.8.99", "1.8.100-SNAPSHOT"),
                d("1.8.0-alpha-1-SNAPSHOT", "1.8.0-alpha-1", "1.8.0-alpha-2-SNAPSHOT"),
                d("1.8.0-alpha-9-SNAPSHOT", "1.8.0-alpha-9", "1.8.0-alpha-10-SNAPSHOT"),
                d("1.8.0-alpha-SNAPSHOT", "1.8.0-alpha", "1.8.0-beta-1-SNAPSHOT"),
                d("1.8.0.alpha-SNAPSHOT", "1.8.0.alpha", "1.8.0.beta.1-SNAPSHOT"),
                d("1.8.0-1.7.1.0-alpha-1-SNAPSHOT", "1.8.0-1.7.1.0-alpha-1", "1.8.0-1.7.1.0-alpha-2-SNAPSHOT"),
        };
    }

    private static Data d(String before, String release, String after) {
        return new Data(before, release, after);
    }

    private static class Data {
        final String before, release, after;

        private Data(String before, String release, String after) {
            this.before = before;
            this.release = release;
            this.after = after;
        }
    }
}
