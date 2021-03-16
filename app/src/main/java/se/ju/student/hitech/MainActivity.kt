package se.ju.student.hitech

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import se.ju.student.hitech.chat.ChatRepository
import se.ju.student.hitech.chat.fragments.ActiveChatsFragmentAdmin
import se.ju.student.hitech.chat.fragments.ContactCaseFragment
import se.ju.student.hitech.chat.fragments.ContactFragment
import se.ju.student.hitech.test.TestFragment

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG_FRAGMENT_CREATE_NEW_EVENT = "TAG_FRAGMENT_NEW_EVENT"
        const val TAG_FRAGMENT_SHOP = "TAG_FRAGMENT_SHOP"
        const val TAG_FRAGMENT_EVENTS = "TAG_FRAGMENT_EVENTS"
        const val TAG_FRAGMENT_NEWS = "TAG_FRAGMENT_NEWS"
        const val TAG_FRAGMENT_CONTACT_CASE = "TAG_FRAGMENT_CONTACT_CASE"
        const val TAG_FRAGMENT_CONTACT_ADMIN_VIEW = "TAG_FRAGMENT_CONTACT_ADMIN_VIEW"
        const val TAG_FRAGMENT_CONTACT = "TAG_FRAGMENT_CONTACT"
        const val TAG_FRAGMENT_ADMIN_LOGIN = "TAG_FRAGMENT_ADMIN_LOGIN"
        const val TAG_FRAGMENT_ABOUT = "TAG_FRAGMENT_ABOUT"
        const val TAG_MAIN_ACTIVITY = "MainActivity"
        const val TAG_ADMIN_EMAIL = "it.hitech@js.ju.se"
        const val TOPIC_NEWS = "/topics/news"
        const val TAG_REGISTER_USER = "TAG_FRAGMENT_REGISTER_USER"
        const val TAG_USER_PAGE = "TAG_FRAGMENT_USER_PAGE"
        const val TAG_TEST = "TAG_TEST"
    }

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_HITECH)
        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setLogo(R.drawable.ic_hitech_logo_20)
        supportActionBar?.setDisplayUseLogoEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager

                .beginTransaction()
                .add(R.id.fragment_container, NewsFragment(), TAG_FRAGMENT_NEWS)
                .add(R.id.fragment_container, ContactCaseFragment(), TAG_FRAGMENT_CONTACT_CASE)
                .add(
                    R.id.fragment_container,
                    ActiveChatsFragmentAdmin(),
                    TAG_FRAGMENT_CONTACT_ADMIN_VIEW
                )
                .add(R.id.fragment_container, AdminLoginFragment(), TAG_FRAGMENT_ADMIN_LOGIN)
                .add(R.id.fragment_container, AboutFragment(), TAG_FRAGMENT_ABOUT)
                .add(R.id.fragment_container, EventsFragment(), TAG_FRAGMENT_EVENTS)
                .add(R.id.fragment_container, ShopFragment(), TAG_FRAGMENT_SHOP)
                .add(R.id.fragment_container, RegisterUserFragment(), TAG_REGISTER_USER)
                .add(R.id.fragment_container, ContactFragment(), TAG_FRAGMENT_CONTACT)
                .add(R.id.fragment_container, UserPageFragment(), TAG_USER_PAGE)
                .add(
                    R.id.fragment_container,
                    CreateNewEventFragment(),
                    TAG_FRAGMENT_CREATE_NEW_EVENT
                )
                .add(R.id.fragment_container, TestFragment(), TAG_TEST)
                .commitNow()
            changeToFragment(TAG_FRAGMENT_NEWS)
        }

        // subscribe all users to news notifications
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_NEWS)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNav.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_news -> changeToFragment(TAG_FRAGMENT_NEWS)
                R.id.nav_events -> changeToFragment(TAG_FRAGMENT_EVENTS)
                R.id.nav_shop -> changeToFragment(TAG_FRAGMENT_SHOP)
                R.id.nav_contact -> {
                    checkWhichContactFragmentToShow { fragment ->
                        changeToFragment(fragment)
                    }
                }
                // R.id.nav_contact -> changeToFragment(TAG_FRAGMENT_CONTACT_CASE) if logged in
            }
            true
        }
    }

    fun createNotification(title: String, message: String, topic: String) {
        PushNotification(
            NotificationData(title, message),
            topic
        ).also {
            sendNotification(it)
        }
    }

    @SuppressLint("HardwareIds")
    private fun checkWhichContactFragmentToShow(callback: (String) -> Unit) {
        if (UserRepository().checkIfLoggedIn()) {
            changeToFragment(TAG_FRAGMENT_CONTACT_ADMIN_VIEW)
        } else {
            val androidID = Settings.Secure.getString(
                this.contentResolver,
                Settings.Secure.ANDROID_ID
            )

            ChatRepository().getChatIDWithAndroidID(androidID) { result, chatID ->
                when (result) {

                    "successful" -> {
                        ChatRepository().setCurrentChatID(chatID)
                        reloadContactFragment()
                        callback(TAG_FRAGMENT_CONTACT)
                    }

                    "notFound" -> {
                        callback(TAG_FRAGMENT_CONTACT_CASE)
                    }
                    "internalError" -> makeToast("Something went wrong, check your internet connection and try again.")
                }
            }
        }
    }

    fun reloadContactFragment(){

        val contactFragment = supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_CONTACT)
        if (contactFragment != null){
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.remove(contactFragment)
            fragmentTransaction.add(R.id.fragment_container, ContactFragment(), TAG_FRAGMENT_CONTACT)
            fragmentTransaction.commit()
        }
    }

    private fun sendNotification(notification: PushNotification) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.postNotification(notification)

                if (response.isSuccessful) {
                    Log.d(TAG_MAIN_ACTIVITY, "SUCCESSFUL")
                } else {
                    Log.e(TAG_MAIN_ACTIVITY, response.errorBody().toString())
                }
            } catch (e: Exception) {
                Log.e(TAG_MAIN_ACTIVITY, e.toString())
            }
        }

    private fun BottomNavigationView.uncheckAllItems() {
        menu.setGroupCheckable(0, true, false)
        for (i in 0 until menu.size()) {
            menu.getItem(i).isChecked = false
        }
        menu.setGroupCheckable(0, true, true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        return when (item.itemId) {
            R.id.nav_login -> {
                bottomNav.uncheckAllItems()
                if (UserRepository().checkIfLoggedIn()) {
                    changeToFragment(TAG_USER_PAGE)
                } else {
                    changeToFragment(TAG_FRAGMENT_ADMIN_LOGIN)
                }
                return true
            }
            R.id.nav_about -> {
                bottomNav.uncheckAllItems()
                changeToFragment(TAG_FRAGMENT_ABOUT)
                return true
            }
            R.id.nav_problem -> {
                showReportProblemAlert()
                return true
            }
            R.id.test -> {
                changeToFragment(TAG_TEST)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun showReportProblemAlert() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_report_problem, null)

        AlertDialog.Builder(this)
            .setTitle(R.string.problem)
            .setView(dialogView)
            .setPositiveButton(
                R.string.send
            ) { dialog, whichButton ->
                // Send email from users input
                val mail = dialogView.findViewById<EditText>(R.id.edittext_problem).text
                sendEmail(mail)
            }.setNegativeButton(
                R.string.cancel
            ) { dialog, whichButton ->
                // Do nothing
            }.show()
    }

    private fun sendEmail(message: Editable?) {
        val subject = "Report problem HI TECH Android application"

        // email intent to HI TECH IT Manager
        val emailIntent =
            Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", TAG_ADMIN_EMAIL, null))

        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        emailIntent.putExtra(Intent.EXTRA_TEXT, message.toString())
        try {
            (Intent.createChooser(emailIntent, "Choose email client.."))
            startActivity(emailIntent)
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
    }


    fun changeToFragment(fragment_tag: String) {
        with(supportFragmentManager.beginTransaction()) {

            for (fragment in supportFragmentManager.fragments) {
                hide(fragment)
            }

            show(supportFragmentManager.findFragmentByTag(fragment_tag)!!)
            commit()
        }
    }


    fun makeToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }


}