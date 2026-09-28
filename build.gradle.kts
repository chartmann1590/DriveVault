plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.10" apply false
    id("com.google.firebase.crashlytics") version "3.0.7" apply false
    id("com.google.firebase.firebase-perf") version "2.0.2" apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}

// The firebase-crashlytics-buildtools plugin pulls in old, CVE-flagged
// versions of netty/bouncycastle/commons-compress/jose4j/jdom2 via grpc.
// These are buildscript-classpath-only (never shipped in the APK), but
// force them to patched releases so CodeQL/Dependabot stop flagging them.
buildscript {
    configurations.classpath {
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
