package ir.kindnesswall

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.iid.FirebaseInstanceId
import ir.kindnesswall.data.local.AppPref
import ir.kindnesswall.data.local.UserInfoPref
import ir.kindnesswall.data.repository.UserRepo
import ir.kindnesswall.utils.LocaleHelper
import ir.kindnesswall.utils.widgets.GetInputDialog
import org.koin.android.ext.android.inject


/**
 * Created by farshid.abazari since 2019-10-31
 *
 * Usage:
 *
 * How to call:
 *
 * Useful parameter:
 *
 */

@SuppressLint("Registered")
abstract class BaseActivity : AppCompatActivity() {
    private val userRepo: UserRepo by inject()

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(
            LocaleHelper.onAttach(
                newBase,
                AppPref.currentLocale
            )
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onResume() {
        super.onResume()

        if (UserInfoPref.bearerToken.isNotEmpty()) {
            if (UserInfoPref.fireBaseToken.isEmpty()) {
                FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { result ->
                    if (result.isSuccessful) {
                        val token = result.result?.token.toString()
                        if (token.isNotEmpty()) {
                            UserInfoPref.fireBaseToken = token
                            userRepo.registerFirebaseToken()
                        }
                    }
                }
            }
        }

        if (AppPref.shouldUpdatedFireBaseToken) {
            userRepo.registerFirebaseToken()
        }
    }

    abstract fun configureViews(savedInstanceState: Bundle?)

    fun showToastMessage(message: String) {
        if (message.isNotEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private var promptDialog: android.app.AlertDialog? = null

    fun showPromptDialog(
        title: String? = null,
        messageToShow: String? = null,
        positiveButtonText: String = getString(R.string.yes),
        negativeButtonText: String = getString(R.string.no),
        positiveButtonTextColor: Int = ContextCompat.getColor(this, R.color.primaryTextColor),
        negativeButtonTextColor: Int = ContextCompat.getColor(this, R.color.secondaryTextColor),
        showPositiveButton: Boolean = true,
        showNegativeButton: Boolean = true,
        cancelable: Boolean = true,
        canceledOnTouchOutside: Boolean = true,
        showCheckBox: Boolean = false,
        onPositiveClickCallback: ((Boolean) -> Unit)? = null,
        onNegativeClickCallback: (() -> Unit)? = null,
        onDismissCallback: (() -> Unit)? = null,
        checkBoxMessage: String? = null
    ) {
        var checkBoxChecked = false

        if (promptDialog == null) {
//            var checkBoxView: View? = null
//            if (showCheckBox) {
//                checkBoxView = View.inflate(this, R.layout.prompt_dialog_with_check_box, null)
//                val checkBox = checkBoxView.findViewById(R.id.alertDialogCheckBox) as CheckBox
//                checkBox.text =
//                    checkBoxMessage ?: getString(R.string.request_reject_popup__dont_show_again_box)
//
//                checkBox.setOnCheckedChangeListener { _, b ->
//                    checkBoxChecked = b
//                }
//            }

            promptDialog = android.app.AlertDialog.Builder(this).apply {
                this.setMessage(messageToShow)
                this.setTitle(title)
                if (showPositiveButton)
                    this.setPositiveButton(positiveButtonText) { dialog, _ ->
                        onPositiveClickCallback?.invoke(checkBoxChecked)
                        dialog.dismiss()
                    }
                if (showNegativeButton)
                    this.setNegativeButton(negativeButtonText) { dialog, _ ->
                        if (onNegativeClickCallback == null) {
                            dialog.dismiss()
                            promptDialog = null
                        } else {
                            onNegativeClickCallback.invoke()
                        }
                    }
                this.setOnDismissListener {
                    promptDialog = null
                    onDismissCallback?.invoke()
                }
                this.setOnCancelListener {
                    promptDialog = null
                    onDismissCallback?.invoke()
                }
            }.also {
                if (showCheckBox) {
//                    it.setView(checkBoxView)
                }
            }.create()

            promptDialog?.show()
            promptDialog?.setCancelable(cancelable)
            promptDialog?.setCanceledOnTouchOutside(canceledOnTouchOutside)
            if (showNegativeButton)
                promptDialog?.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                    negativeButtonTextColor
                )
            if (showPositiveButton)
                promptDialog?.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                    positiveButtonTextColor
                )
        }
    }

    fun dismissPromptDialog() {
        promptDialog?.dismiss()
        promptDialog = null
    }

    fun showGetInputDialog(
        arguments: Bundle?,
        approveListener: ((String) -> Unit)? = null,
        cancelListener: (() -> Unit)? = null
    ) {
        val dialog = GetInputDialog()
        supportFragmentManager.let {
            val prev = it.findFragmentByTag("getInputDialog")
            if (prev != null) {
                it.beginTransaction()
                    .remove(prev)
                    .addToBackStack(null)
            }
            dialog.arguments = arguments
            dialog.onApproveListener = approveListener
            dialog.onRefuseListener = cancelListener

            dialog.show(it, "questionDialog")
        }
    }

    fun showProgressDialog() {

    }

    fun dismissProgressDialog() {

    }
}