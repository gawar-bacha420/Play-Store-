package com.ultimatecloner.safe

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ultimatecloner.safe.CloneEngine.ContainerManager
import com.ultimatecloner.safe.Feedback.FeedbackManager
import com.ultimatecloner.safe.Spoofing.IdentityManager
import com.ultimatecloner.safe.Utils.PolicyUtils
import com.ultimatecloner.safe.databinding.ActivityMainBinding
import com.ultimatecloner.safe.CloneEngine.ClonedApp
import com.ultimatecloner.safe.CloneEngine.CloneAdapter
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var containerManager: ContainerManager
    private lateinit var identityManager: IdentityManager
    private lateinit var feedbackManager: FeedbackManager
    
    private val cloneList = mutableListOf<ClonedApp>()
    private lateinit var adapter: CloneAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize managers
        containerManager = ContainerManager(this)
        identityManager = IdentityManager(this)
        feedbackManager = FeedbackManager(this)
        
        // Show privacy policy on first launch (REQUIRED for Play Store)
        PolicyUtils.showPrivacyPolicyOnFirstLaunch(this)
        
        setupUI()
        loadClones()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Container Pro"
        
        adapter = CloneAdapter(cloneList) { clonedApp ->
            showCloneOptions(clonedApp)
        }
        
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        
        binding.fabAdd.setOnClickListener {
            showAppSelectionDialog()
        }
        
        binding.buttonFeedback.setOnClickListener {
            showFeedbackDialog()
        }
    }

    private fun loadClones() {
        cloneList.clear()
        cloneList.addAll(containerManager.getInstalledContainers())
        adapter.notifyDataSetChanged()
    }

    private fun showAppSelectionDialog() {
        // Get all installed apps
        val apps = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }
        
        // Filter out system apps and our own app
        val filteredApps = apps.filter { 
            (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 &&
            it.packageName != packageName
        }
        
        val appNames = filteredApps.map { 
            it.loadLabel(packageManager).toString()
        }.toTypedArray()
        
        val packageNames = filteredApps.map { it.packageName }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Select App to Containerize")
            .setItems(appNames) { _, which ->
                val pkg = packageNames[which]
                val name = appNames[which]
                createContainer(pkg, name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createContainer(packageName: String, appName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Step 1: Create sandbox container (NOT modifying original APK)
                val containerId = containerManager.createContainer(packageName)
                
                // Step 2: Generate runtime identity for this container
                identityManager.generateIdentity(containerId)
                
                withContext(Dispatchers.Main) {
                    val clonedApp = ClonedApp(
                        id = containerId,
                        packageName = packageName,
                        appName = appName,
                        installTime = System.currentTimeMillis()
                    )
                    cloneList.add(clonedApp)
                    adapter.notifyItemInserted(cloneList.size - 1)
                    
                    Snackbar.make(
                        binding.root,
                        "$appName containerized successfully!",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        "Error: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showCloneOptions(clonedApp: ClonedApp) {
        val options = arrayOf(
            "Launch Container",
            "Regenerate Identity",
            "Clear Container Data",
            "Remove Container"
        )
        
        MaterialAlertDialogBuilder(this)
            .setTitle(clonedApp.appName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> containerManager.launchContainer(clonedApp.id, clonedApp.packageName)
                    1 -> {
                        identityManager.regenerateIdentity(clonedApp.id)
                        Toast.makeText(this, "New identity generated!", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        containerManager.clearContainerData(clonedApp.id)
                        Snackbar.make(binding.root, "Container data cleared", Snackbar.LENGTH_SHORT).show()
                    }
                    3 -> {
                        containerManager.removeContainer(clonedApp.id)
                        cloneList.remove(clonedApp)
                        adapter.notifyDataSetChanged()
                        Snackbar.make(binding.root, "Container removed", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun showFeedbackDialog() {
        val input = com.google.android.material.textfield.TextInputEditText(this)
        input.hint = "Describe your experience..."
        input.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Send Feedback")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val message = input.text.toString()
                if (message.isNotEmpty()) {
                    feedbackManager.submitFeedback(message)
                    Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
