package com.mobileide.domain.service

import com.mobileide.domain.model.template.ProjectTemplate
import com.mobileide.domain.model.template.TemplateFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateService @Inject constructor() {
    
    fun getTemplates(): List<ProjectTemplate> {
        return listOf(
            createEmptyProject(),
            createBasicActivity(),
            createNavigationDrawer()
        )
    }
    
    fun getTemplateById(id: Int): ProjectTemplate? {
        return getTemplates().find { it.id == id }
    }
    
    private fun createEmptyProject(): ProjectTemplate {
        return ProjectTemplate(
            id = 0,
            name = "Empty Project",
            description = "A basic Android project with minimal setup",
            files = listOf(
                TemplateFile(
                    path = "src/main/AndroidManifest.xml",
                    content = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                            package="{{PACKAGE_NAME}}">
                            
                            <application
                                android:allowBackup="true"
                                android:icon="@mipmap/ic_launcher"
                                android:label="{{PROJECT_NAME}}"
                                android:roundIcon="@mipmap/ic_launcher_round"
                                android:supportsRtl="true"
                                android:theme="@style/Theme.AppCompat.Light.DarkActionBar">
                                
                                <activity android:name=".MainActivity"
                                    android:exported="true">
                                    <intent-filter>
                                        <action android:name="android.intent.action.MAIN" />
                                        <category android:name="android.intent.category.LAUNCHER" />
                                    </intent-filter>
                                </activity>
                                
                            </application>
                            
                        </manifest>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/java/{{PACKAGE_PATH}}/MainActivity.kt",
                    content = """
                        package {{PACKAGE_NAME}}
                        
                        import android.os.Bundle
                        import androidx.appcompat.app.AppCompatActivity
                        
                        class MainActivity : AppCompatActivity() {
                            
                            override fun onCreate(savedInstanceState: Bundle?) {
                                super.onCreate(savedInstanceState)
                                setContentView(R.layout.activity_main)
                            }
                        }
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/layout/activity_main.xml",
                    content = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <androidx.constraintlayout.widget.ConstraintLayout 
                            xmlns:android="http://schemas.android.com/apk/res/android"
                            xmlns:app="http://schemas.android.com/apk/res-auto"
                            xmlns:tools="http://schemas.android.com/tools"
                            android:layout_width="match_parent"
                            android:layout_height="match_parent"
                            tools:context=".MainActivity">
                            
                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Hello World!"
                                app:layout_constraintBottom_toBottomOf="parent"
                                app:layout_constraintEnd_toEndOf="parent"
                                app:layout_constraintStart_toStartOf="parent"
                                app:layout_constraintTop_toTopOf="parent" />
                                
                        </androidx.constraintlayout.widget.ConstraintLayout>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/values/strings.xml",
                    content = """
                        <resources>
                            <string name="app_name">{{PROJECT_NAME}}</string>
                        </resources>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/values/colors.xml",
                    content = """
                        <resources>
                            <color name="colorPrimary">#6200EE</color>
                            <color name="colorPrimaryDark">#3700B3</color>
                            <color name="colorAccent">#03DAC5</color>
                        </resources>
                    """.trimIndent()
                )
            )
        )
    }
    
    private fun createBasicActivity(): ProjectTemplate {
        return ProjectTemplate(
            id = 1,
            name = "Basic Activity",
            description = "A project with a basic activity and a floating action button",
            files = listOf(
                TemplateFile(
                    path = "src/main/AndroidManifest.xml",
                    content = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                            package="{{PACKAGE_NAME}}">
                            
                            <application
                                android:allowBackup="true"
                                android:icon="@mipmap/ic_launcher"
                                android:label="{{PROJECT_NAME}}"
                                android:roundIcon="@mipmap/ic_launcher_round"
                                android:supportsRtl="true"
                                android:theme="@style/Theme.AppCompat.Light.DarkActionBar">
                                
                                <activity android:name=".MainActivity"
                                    android:exported="true">
                                    <intent-filter>
                                        <action android:name="android.intent.action.MAIN" />
                                        <category android:name="android.intent.category.LAUNCHER" />
                                    </intent-filter>
                                </activity>
                                
                            </application>
                            
                        </manifest>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/java/{{PACKAGE_PATH}}/MainActivity.kt",
                    content = """
                        package {{PACKAGE_NAME}}
                        
                        import android.os.Bundle
                        import android.view.Menu
                        import android.view.MenuItem
                        import android.widget.Toast
                        import androidx.appcompat.app.AppCompatActivity
                        import com.google.android.material.floatingactionbutton.FloatingActionButton
                        
                        class MainActivity : AppCompatActivity() {
                            
                            override fun onCreate(savedInstanceState: Bundle?) {
                                super.onCreate(savedInstanceState)
                                setContentView(R.layout.activity_main)
                                setSupportActionBar(findViewById(R.id.toolbar))
                                
                                findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
                                    Toast.makeText(this, "Replace with your own action", Toast.LENGTH_SHORT).show()
                                }
                            }
                            
                            override fun onCreateOptionsMenu(menu: Menu): Boolean {
                                menuInflater.inflate(R.menu.menu_main, menu)
                                return true
                            }
                            
                            override fun onOptionsItemSelected(item: MenuItem): Boolean {
                                return when (item.itemId) {
                                    R.id.action_settings -> {
                                        Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                                        true
                                    }
                                    else -> super.onOptionsItemSelected(item)
                                }
                            }
                        }
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/layout/activity_main.xml",
                    content = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <androidx.coordinatorlayout.widget.CoordinatorLayout 
                            xmlns:android="http://schemas.android.com/apk/res/android"
                            xmlns:app="http://schemas.android.com/apk/res-auto"
                            xmlns:tools="http://schemas.android.com/tools"
                            android:layout_width="match_parent"
                            android:layout_height="match_parent"
                            tools:context=".MainActivity">
                            
                            <com.google.android.material.appbar.AppBarLayout
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:theme="@style/ThemeOverlay.AppCompat.Dark.ActionBar">
                                
                                <androidx.appcompat.widget.Toolbar
                                    android:id="@+id/toolbar"
                                    android:layout_width="match_parent"
                                    android:layout_height="?attr/actionBarSize"
                                    android:background="?attr/colorPrimary"
                                    app:popupTheme="@style/ThemeOverlay.AppCompat.Light" />
                                    
                            </com.google.android.material.appbar.AppBarLayout>
                            
                            <include layout="@layout/content_main" />
                            
                            <com.google.android.material.floatingactionbutton.FloatingActionButton
                                android:id="@+id/fab"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:layout_gravity="bottom|end"
                                android:layout_margin="16dp"
                                app:srcCompat="@android:drawable/ic_dialog_email" />
                                
                        </androidx.coordinatorlayout.widget.CoordinatorLayout>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/layout/content_main.xml",
                    content = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <androidx.constraintlayout.widget.ConstraintLayout 
                            xmlns:android="http://schemas.android.com/apk/res/android"
                            xmlns:app="http://schemas.android.com/apk/res-auto"
                            xmlns:tools="http://schemas.android.com/tools"
                            android:layout_width="match_parent"
                            android:layout_height="match_parent"
                            app:layout_behavior="@string/appbar_scrolling_view_behavior"
                            tools:context=".MainActivity"
                            tools:showIn="@layout/activity_main">
                            
                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Hello World!"
                                app:layout_constraintBottom_toBottomOf="parent"
                                app:layout_constraintEnd_toEndOf="parent"
                                app:layout_constraintStart_toStartOf="parent"
                                app:layout_constraintTop_toTopOf="parent" />
                                
                        </androidx.constraintlayout.widget.ConstraintLayout>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/menu/menu_main.xml",
                    content = """
                        <menu xmlns:android="http://schemas.android.com/apk/res/android"
                            xmlns:app="http://schemas.android.com/apk/res-auto"
                            xmlns:tools="http://schemas.android.com/tools"
                            tools:context=".MainActivity">
                            <item
                                android:id="@+id/action_settings"
                                android:orderInCategory="100"
                                android:title="Settings"
                                app:showAsAction="never" />
                        </menu>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/values/strings.xml",
                    content = """
                        <resources>
                            <string name="app_name">{{PROJECT_NAME}}</string>
                            <string name="action_settings">Settings</string>
                        </resources>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/values/colors.xml",
                    content = """
                        <resources>
                            <color name="colorPrimary">#6200EE</color>
                            <color name="colorPrimaryDark">#3700B3</color>
                            <color name="colorAccent">#03DAC5</color>
                        </resources>
                    """.trimIndent()
                )
            )
        )
    }
    
    private fun createNavigationDrawer(): ProjectTemplate {
        return ProjectTemplate(
            id = 2,
            name = "Navigation Drawer",
            description = "A project with a navigation drawer and multiple fragments",
            files = listOf(
                TemplateFile(
                    path = "src/main/AndroidManifest.xml",
                    content = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                            package="{{PACKAGE_NAME}}">
                            
                            <application
                                android:allowBackup="true"
                                android:icon="@mipmap/ic_launcher"
                                android:label="{{PROJECT_NAME}}"
                                android:roundIcon="@mipmap/ic_launcher_round"
                                android:supportsRtl="true"
                                android:theme="@style/Theme.AppCompat.Light.DarkActionBar">
                                
                                <activity android:name=".MainActivity"
                                    android:exported="true">
                                    <intent-filter>
                                        <action android:name="android.intent.action.MAIN" />
                                        <category android:name="android.intent.category.LAUNCHER" />
                                    </intent-filter>
                                </activity>
                                
                            </application>
                            
                        </manifest>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/java/{{PACKAGE_PATH}}/MainActivity.kt",
                    content = """
                        package {{PACKAGE_NAME}}
                        
                        import android.os.Bundle
                        import android.view.Menu
                        import android.view.MenuItem
                        import androidx.appcompat.app.ActionBarDrawerToggle
                        import androidx.appcompat.app.AppCompatActivity
                        import androidx.appcompat.widget.Toolbar
                        import androidx.core.view.GravityCompat
                        import androidx.drawerlayout.widget.DrawerLayout
                        import androidx.fragment.app.Fragment
                        import com.google.android.material.navigation.NavigationView
                        
                        class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
                            
                            private lateinit var drawerLayout: DrawerLayout
                            
                            override fun onCreate(savedInstanceState: Bundle?) {
                                super.onCreate(savedInstanceState)
                                setContentView(R.layout.activity_main)
                                
                                val toolbar: Toolbar = findViewById(R.id.toolbar)
                                setSupportActionBar(toolbar)
                                
                                drawerLayout = findViewById(R.id.drawer_layout)
                                val navView: NavigationView = findViewById(R.id.nav_view)
                                
                                val toggle = ActionBarDrawerToggle(
                                    this, drawerLayout, toolbar,
                                    R.string.navigation_drawer_open,
                                    R.string.navigation_drawer_close
                                )
                                drawerLayout.addDrawerListener(toggle)
                                toggle.syncState()
                                
                                navView.setNavigationItemSelectedListener(this)
                                
                                // Set the default fragment
                                if (savedInstanceState == null) {
                                    supportFragmentManager.beginTransaction()
                                        .replace(R.id.fragment_container, HomeFragment())
                                        .commit()
                                    navView.setCheckedItem(R.id.nav_home)
                                }
                            }
                            
                            override fun onBackPressed() {
                                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                                    drawerLayout.closeDrawer(GravityCompat.START)
                                } else {
                                    super.onBackPressed()
                                }
                            }
                            
                            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                                var fragment: Fragment? = null
                                
                                when (item.itemId) {
                                    R.id.nav_home -> fragment = HomeFragment()
                                    R.id.nav_gallery -> fragment = GalleryFragment()
                                    R.id.nav_slideshow -> fragment = SlideshowFragment()
                                }
                                
                                fragment?.let {
                                    supportFragmentManager.beginTransaction()
                                        .replace(R.id.fragment_container, it)
                                        .commit()
                                }
                                
                                drawerLayout.closeDrawer(GravityCompat.START)
                                return true
                            }
                        }
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/java/{{PACKAGE_PATH}}/HomeFragment.kt",
                    content = """
                        package {{PACKAGE_NAME}}
                        
                        import android.os.Bundle
                        import android.view.LayoutInflater
                        import android.view.View
                        import android.view.ViewGroup
                        import androidx.fragment.app.Fragment
                        
                        class HomeFragment : Fragment() {
                            
                            override fun onCreateView(
                                inflater: LayoutInflater,
                                container: ViewGroup?,
                                savedInstanceState: Bundle?
                            ): View? {
                                return inflater.inflate(R.layout.fragment_home, container, false)
                            }
                        }
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/java/{{PACKAGE_PATH}}/GalleryFragment.kt",
                    content = """
                        package {{PACKAGE_NAME}}
                        
                        import android.os.Bundle
                        import android.view.LayoutInflater
                        import android.view.View
                        import android.view.ViewGroup
                        import androidx.fragment.app.Fragment
                        
                        class GalleryFragment : Fragment() {
                            
                            override fun onCreateView(
                                inflater: LayoutInflater,
                                container: ViewGroup?,
                                savedInstanceState: Bundle?
                            ): View? {
                                return inflater.inflate(R.layout.fragment_gallery, container, false)
                            }
                        }
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/java/{{PACKAGE_PATH}}/SlideshowFragment.kt",
                    content = """
                        package {{PACKAGE_NAME}}
                        
                        import android.os.Bundle
                        import android.view.LayoutInflater
                        import android.view.View
                        import android.view.ViewGroup
                        import androidx.fragment.app.Fragment
                        
                        class SlideshowFragment : Fragment() {
                            
                            override fun onCreateView(
                                inflater: LayoutInflater,
                                container: ViewGroup?,
                                savedInstanceState: Bundle?
                            ): View? {
                                return inflater.inflate(R.layout.fragment_slideshow, container, false)
                            }
                        }
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/layout/activity_main.xml",
                    content = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <androidx.drawerlayout.widget.DrawerLayout 
                            xmlns:android="http://schemas.android.com/apk/res/android"
                            xmlns:app="http://schemas.android.com/apk/res-auto"
                            xmlns:tools="http://schemas.android.com/tools"
                            android:id="@+id/drawer_layout"
                            android:layout_width="match_parent"
                            android:layout_height="match_parent"
                            android:fitsSystemWindows="true"
                            tools:openDrawer="start">
                            
                            <LinearLayout
                                android:layout_width="match_parent"
                                android:layout_height="match_parent"
                                android:orientation="vertical">
                                
                                <androidx.appcompat.widget.Toolbar
                                    android:id="@+id/toolbar"
                                    android:layout_width="match_parent"
                                    android:layout_height="?attr/actionBarSize"
                                    android:background="?attr/colorPrimary"
                                    android:theme="@style/ThemeOverlay.AppCompat.Dark.ActionBar"
                                    app:popupTheme="@style/ThemeOverlay.AppCompat.Light" />
                                    
                                <FrameLayout
                                    android:id="@+id/fragment_container"
                                    android:layout_width="match_parent"
                                    android:layout_height="match_parent" />
                                    
                            </LinearLayout>
                            
                            <com.google.android.material.navigation.NavigationView
                                android:id="@+id/nav_view"
                                android:layout_width="wrap_content"
                                android:layout_height="match_parent"
                                android:layout_gravity="start"
                                android:fitsSystemWindows="true"
                                app:headerLayout="@layout/nav_header"
                                app:menu="@menu/drawer_menu" />
                                
                        </androidx.drawerlayout.widget.DrawerLayout>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/layout/nav_header.xml",
                    content = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <LinearLayout 
                            xmlns:android="http://schemas.android.com/apk/res/android"
                            android:layout_width="match_parent"
                            android:layout_height="176dp"
                            android:background="@color/colorPrimary"
                            android:gravity="bottom"
                            android:orientation="vertical"
                            android:padding="16dp"
                            android:theme="@style/ThemeOverlay.AppCompat.Dark">
                            
                            <ImageView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:src="@android:drawable/sym_def_app_icon" />
                                
                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:paddingTop="8dp"
                                android:text="{{PROJECT_NAME}}"
                                android:textAppearance="@style/TextAppearance.AppCompat.Body1" />
                                
                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="{{PACKAGE_NAME}}" />
                                
                        </LinearLayout>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/layout/fragment_home.xml",
                    content = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <androidx.constraintlayout.widget.ConstraintLayout 
                            xmlns:android="http://schemas.android.com/apk/res/android"
                            xmlns:app="http://schemas.android.com/apk/res-auto"
                            android:layout_width="match_parent"
                            android:layout_height="match_parent">
                            
                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Home Fragment"
                                android:textSize="24sp"
                                app:layout_constraintBottom_toBottomOf="parent"
                                app:layout_constraintEnd_toEndOf="parent"
                                app:layout_constraintStart_toStartOf="parent"
                                app:layout_constraintTop_toTopOf="parent" />
                                
                        </androidx.constraintlayout.widget.ConstraintLayout>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/layout/fragment_gallery.xml",
                    content = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <androidx.constraintlayout.widget.ConstraintLayout 
                            xmlns:android="http://schemas.android.com/apk/res/android"
                            xmlns:app="http://schemas.android.com/apk/res-auto"
                            android:layout_width="match_parent"
                            android:layout_height="match_parent">
                            
                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Gallery Fragment"
                                android:textSize="24sp"
                                app:layout_constraintBottom_toBottomOf="parent"
                                app:layout_constraintEnd_toEndOf="parent"
                                app:layout_constraintStart_toStartOf="parent"
                                app:layout_constraintTop_toTopOf="parent" />
                                
                        </androidx.constraintlayout.widget.ConstraintLayout>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/layout/fragment_slideshow.xml",
                    content = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <androidx.constraintlayout.widget.ConstraintLayout 
                            xmlns:android="http://schemas.android.com/apk/res/android"
                            xmlns:app="http://schemas.android.com/apk/res-auto"
                            android:layout_width="match_parent"
                            android:layout_height="match_parent">
                            
                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Slideshow Fragment"
                                android:textSize="24sp"
                                app:layout_constraintBottom_toBottomOf="parent"
                                app:layout_constraintEnd_toEndOf="parent"
                                app:layout_constraintStart_toStartOf="parent"
                                app:layout_constraintTop_toTopOf="parent" />
                                
                        </androidx.constraintlayout.widget.ConstraintLayout>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/menu/drawer_menu.xml",
                    content = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <menu xmlns:android="http://schemas.android.com/apk/res/android"
                            xmlns:tools="http://schemas.android.com/tools"
                            tools:showIn="navigation_view">
                            
                            <group android:checkableBehavior="single">
                                <item
                                    android:id="@+id/nav_home"
                                    android:icon="@android:drawable/ic_menu_compass"
                                    android:title="Home" />
                                <item
                                    android:id="@+id/nav_gallery"
                                    android:icon="@android:drawable/ic_menu_gallery"
                                    android:title="Gallery" />
                                <item
                                    android:id="@+id/nav_slideshow"
                                    android:icon="@android:drawable/ic_menu_slideshow"
                                    android:title="Slideshow" />
                            </group>
                            
                            <item android:title="Communicate">
                                <menu>
                                    <item
                                        android:id="@+id/nav_share"
                                        android:icon="@android:drawable/ic_menu_share"
                                        android:title="Share" />
                                    <item
                                        android:id="@+id/nav_send"
                                        android:icon="@android:drawable/ic_menu_send"
                                        android:title="Send" />
                                </menu>
                            </item>
                            
                        </menu>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/values/strings.xml",
                    content = """
                        <resources>
                            <string name="app_name">{{PROJECT_NAME}}</string>
                            <string name="navigation_drawer_open">Open navigation drawer</string>
                            <string name="navigation_drawer_close">Close navigation drawer</string>
                        </resources>
                    """.trimIndent()
                ),
                TemplateFile(
                    path = "src/main/res/values/colors.xml",
                    content = """
                        <resources>
                            <color name="colorPrimary">#6200EE</color>
                            <color name="colorPrimaryDark">#3700B3</color>
                            <color name="colorAccent">#03DAC5</color>
                        </resources>
                    """.trimIndent()
                )
            )
        )
    }
}