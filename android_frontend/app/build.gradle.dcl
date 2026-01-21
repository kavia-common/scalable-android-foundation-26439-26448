androidApplication {
    namespace = "org.example.app"

    dependencies {
        implementation("org.apache.commons:commons-text:1.11.0")
        implementation(project(":utilities"))

        // UI + navigation (Views/XML, no Compose)
        implementation("androidx.appcompat:appcompat:1.6.1")
        implementation("com.google.android.material:material:1.11.0")
        implementation("androidx.core:core-ktx:1.12.0")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")

        // Fragments + MVVM
        implementation("androidx.fragment:fragment-ktx:1.6.2")
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
        implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

        // Jetpack Navigation (Fragment-based)
        implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
        implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

        // Unit testing (JUnit4)
        implementation("junit:junit:4.13.2")
    }
}
