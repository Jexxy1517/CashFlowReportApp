package com.example.cashflowreportapp

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.cashflowreportapp.databinding.FragmentAddTransactionBinding
import com.example.cashflowreportapp.model.Transaction
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AddTransactionFragment : BottomSheetDialogFragment(R.layout.fragment_add_transaction) {

    private lateinit var binding: FragmentAddTransactionBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val viewModel: MainViewModel by activityViewModels() // Mempertahankan ViewModel dari kode lama
    private val accountMap = mutableMapOf<String, String?>()

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivReceiptPreview.setImageURI(uri)
            binding.btnSelectImage.text = "Ganti Gambar"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddTransactionBinding.bind(view)

        setupCategoryDropdown()
        loadAccountsAndSetupDropdown()

        binding.btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.buttonSave.setOnClickListener {
            validateAndSave()
        }
    }

    private fun setupCategoryDropdown() {
        val incomeCategories = listOf("Gaji", "Bonus", "Investasi", "Lainnya")
        val expenseCategories = listOf("Makanan", "Transportasi", "Belanja", "Tagihan", "Pendidikan", "Kesehatan", "Lainnya")

        updateCategoryAdapter(expenseCategories)

        binding.toggleButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btn_income) {
                    updateCategoryAdapter(incomeCategories)
                } else {
                    updateCategoryAdapter(expenseCategories)
                }
            }
        }
    }

    private fun updateCategoryAdapter(items: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        (binding.spinnerCategory as? AutoCompleteTextView)?.setAdapter(adapter)

        // Memastikan text tidak error saat ganti tipe
        val currentText = binding.spinnerCategory.text.toString()
        if (currentText.isNotEmpty() && !items.contains(currentText)) {
            (binding.spinnerCategory as? AutoCompleteTextView)?.setText(items[0], false)
        }
    }

    private fun loadAccountsAndSetupDropdown() {
        val myUid = auth.currentUser?.uid ?: return

        accountMap["Keuangan Pribadi"] = null

        db.collection("savings_groups")
            .whereArrayContains("members", myUid)
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val groupName = doc.getString("name") ?: "Grup Tanpa Nama"
                    val groupId = doc.id
                    accountMap[groupName] = groupId
                }
                setupAccountSpinner()
            }
            .addOnFailureListener {
                setupAccountSpinner()
            }
    }

    private fun setupAccountSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, accountMap.keys.toList())
        val spinner = binding.spinnerAccount as? AutoCompleteTextView
        spinner?.setAdapter(adapter)

        // FITUR LAMA DIKEMBALIKAN: Pre-select akun berdasarkan ViewModel (Current Account)
        val currentContextName = viewModel.currentAccountName.value ?: "Keuangan Pribadi"

        if (accountMap.containsKey(currentContextName)) {
            spinner?.setText(currentContextName, false)
        } else {
            spinner?.setText("Keuangan Pribadi", false)
        }
    }

    private fun validateAndSave() {
        val title = binding.inputTitle.text.toString().trim()
        val amountStr = binding.inputAmount.text.toString().trim()
        val category = binding.spinnerCategory.text.toString()

        if (title.isEmpty() || amountStr.isEmpty() || category.isEmpty()) {
            Toast.makeText(context, "Mohon lengkapi semua data!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonSave.isEnabled = false
        binding.buttonSave.text = "Menyimpan..."

        if (selectedImageUri != null) {
            uploadImageToCloudinary(selectedImageUri!!)
        } else {
            saveTransactionToFirestore(null)
        }
    }

    // FITUR BARU: Kompresi Gambar agar hemat kuota/storage
    private fun compressImage(uri: Uri): File? {
        return try {
            val bitmap = MediaStore.Images.Media.getBitmap(
                requireContext().contentResolver, uri
            )
            // Kompres ke JPEG kualitas 80%
            val file = File(requireContext().cacheDir, "compressed_${UUID.randomUUID()}.jpg")
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            out.flush()
            out.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    // FITUR BARU: Upload ke Cloudinary (menggantikan Firebase Storage)
    private fun uploadImageToCloudinary(uri: Uri) {
        val compressed = compressImage(uri)
        if (compressed == null) {
            Toast.makeText(context, "Gagal mengompres gambar!", Toast.LENGTH_SHORT).show()
            binding.buttonSave.isEnabled = true
            binding.buttonSave.text = "SIMPAN"
            return
        }

        MediaManager.get().upload(compressed.absolutePath)
            .option("upload_preset", "android_unsigned") // Pastikan preset ini ada di dashboard Cloudinary Anda
            .option("folder", "receipts")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                    val url = resultData?.get("secure_url") as? String
                    saveTransactionToFirestore(url)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Toast.makeText(context, "Upload Cloudinary gagal: ${error?.description}", Toast.LENGTH_LONG).show()
                    binding.buttonSave.isEnabled = true
                    binding.buttonSave.text = "SIMPAN"
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }

    private fun saveTransactionToFirestore(imageUrl: String?) {
        val title = binding.inputTitle.text.toString()
        val amount = binding.inputAmount.text.toString().toDoubleOrNull() ?: 0.0
        val category = binding.spinnerCategory.text.toString()
        val type = if (binding.toggleButtonGroup.checkedButtonId == R.id.btn_income) "INCOME" else "EXPENSE"

        val selectedAccountName = binding.spinnerAccount.text.toString()
        val targetGroupId = accountMap[selectedAccountName]

        val transaction = Transaction(
            title = title,
            amount = amount,
            type = type,
            category = category,
            receiptUrl = imageUrl, // URL dari Cloudinary
            date = System.currentTimeMillis(),
            userId = auth.currentUser!!.uid,
            groupId = targetGroupId,
            account = selectedAccountName
        )

        db.collection("transactions")
            .add(transaction)
            .addOnSuccessListener {
                // FITUR LAMA DIKEMBALIKAN: Memanggil NotificationHelper
                val notifHelper = NotificationHelper(requireContext())
                notifHelper.showNotification(
                    "Transaksi Baru",
                    "Berhasil menambahkan $title sebesar Rp $amount"
                )

                Toast.makeText(context, "Transaksi berhasil disimpan!", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error database: ${it.message}", Toast.LENGTH_SHORT).show()
                binding.buttonSave.isEnabled = true
                binding.buttonSave.text = "SIMPAN"
            }
    }
}