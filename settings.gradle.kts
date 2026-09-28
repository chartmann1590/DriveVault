pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// firebase-crashlytics-buildtools pulls old, CVE-flagged versions of
// netty/bouncycastle/commons-compress/jose4j/jdom2 via grpc. These are
// plugin-classpath-only (never shipped in the APK); force patched
// releases here so CodeQL/Dependabot stop flagging them.
buildscript {
    configurations.all {
        resolutionStrategy {
            force(
                "io.netty:netty-common:4.2.16.Final",
                "io.netty:netty-buffer:4.2.16.Final",
                "io.netty:netty-transport:4.2.16.Final",
                "io.netty:netty-resolver:4.2.16.Final",
                "io.netty:netty-codec:4.2.16.Final",
                "io.netty:netty-codec-http:4.2.16.Final",
                "io.netty:netty-codec-http2:4.2.16.Final",
                "io.netty:netty-codec-socks:4.2.16.Final",
                "io.netty:netty-handler:4.2.16.Final",
                "io.netty:netty-handler-proxy:4.2.16.Final",
                "io.netty:netty-transport-native-unix-common:4.2.16.Final",
                "org.bouncycastle:bcprov-jdk18on:1.85",
                "org.bouncycastle:bcpkix-jdk18on:1.85",
                "org.bouncycastle:bcutil-jdk18on:1.85",
                "org.apache.commons:commons-compress:1.28.0",
                "org.bitbucket.b_c:jose4j:0.9.6",
                "org.jdom:jdom2:2.0.6.1"
            )
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DriveVault"
include(":app")
