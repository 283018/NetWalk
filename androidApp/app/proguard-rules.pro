-keep class edu.pwr.zpi.netwalk.iperf.** { *; }
-keep interface edu.pwr.zpi.netwalk.iperf.** { *; }

-keepclassmembers class edu.pwr.zpi.netwalk.iperf.IperfRunner {
    native <methods>;
}
