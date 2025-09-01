package com.example.storemanagerassitent.ui.purchase

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.graphics.drawable.GradientDrawable
import androidx.recyclerview.widget.RecyclerView
import com.example.storemanagerassitent.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class ScannedProductsAdapter(
    private val data: MutableList<BatchScanDialogFragment.ScannedItem>,
    private val onDataChanged: () -> Unit,
    private val categories: List<com.example.storemanagerassitent.data.GoodsCategory> = emptyList()
) : RecyclerView.Adapter<ScannedProductsAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_scanned_product, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = data[position]
        val nameAndSpec = item.name.split(" ", limit = 2)
        holder.tvName.text = nameAndSpec.getOrNull(0) ?: item.name
        holder.tvSpec.text = nameAndSpec.getOrNull(1) ?: ""
        // 显示单价（优先使用本地已存在商品的进价）
        holder.tvPrice.text = "¥0.00"
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val dao = com.example.storemanagerassitent.data.db.ServiceLocator.database.goodsDao()
                val g = dao.findByFullDisplayName(item.name)
                val price = g?.purchasePrice ?: 0.0
                val text = "¥" + DecimalFormat("#0.00").format(price)
                withContext(Dispatchers.Main) {
                    holder.tvPrice.text = text
                }
            } catch (_: Exception) { }
        }
        // 显示单价（优先显示本地已存在商品的进价，否则显示0.00）
        holder.itemView.post {
            try {
                val dao = com.example.storemanagerassitent.data.db.ServiceLocator.database.goodsDao()
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val g = dao.findByFullDisplayName(item.name)
                    val price = g?.purchasePrice ?: 0.0
                    val text = "¥" + java.text.DecimalFormat("#0.00").format(price)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        val tv = holder.itemView.findViewById<TextView>(R.id.tvPrice)
                        tv?.text = text
                    }
                }
            } catch (_: Exception) { }
        }
        // Spinner: 分类（不包含占位项/全部）
        val entries = categories.map { it.name }
        val ctx = holder.itemView.context
        val adapter = ArrayAdapter(ctx, R.layout.item_spinner_category, entries)
        adapter.setDropDownViewResource(R.layout.item_spinner_category_dropdown)
        holder.spinner.adapter = adapter
        val preselect = (categories.indexOfFirst { it.id == item.categoryId }).coerceAtLeast(0)
        holder.spinner.setSelection(preselect)
        // 若分类来自库房（非空），禁用再次选择
        holder.spinner.isEnabled = item.categoryId == null

        holder.spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                val newCategoryId = categories[position].id
                if (newCategoryId != item.categoryId) {
                    val pos = holder.adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return
                    data[pos] = item.copy(categoryId = newCategoryId)
                    onDataChanged()
                }
                updateSpinnerHighlight(holder.spinner, false)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
        updateSpinnerHighlight(holder.spinner, false)
        holder.tvQuantity.text = item.quantity.toString()

        holder.btnIncrease.setOnClickListener {
            data[position] = item.copy(quantity = item.quantity + 1)
            notifyItemChanged(position)
            onDataChanged()
        }

        holder.btnDecrease.setOnClickListener {
            val current = data[position]
            if (current.quantity > 1) {
                data[position] = current.copy(quantity = current.quantity - 1)
                notifyItemChanged(position)
                onDataChanged()
            } else {
                AlertDialog.Builder(holder.itemView.context)
                    .setMessage("是否要从列表中移除该商品？")
                    .setPositiveButton("移除") { _, _ ->
                        data.removeAt(position)
                        notifyItemRemoved(position)
                        onDataChanged()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
    }

    override fun getItemCount(): Int = data.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvSpec: TextView = itemView.findViewById(R.id.tvSpec)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val btnIncrease: ImageButton = itemView.findViewById(R.id.btnIncrease)
        val btnDecrease: ImageButton = itemView.findViewById(R.id.btnDecrease)
        val spinner: Spinner = itemView.findViewById(R.id.spinnerCategory)
    }

    private fun updateSpinnerHighlight(spinner: Spinner, highlight: Boolean) {
        if (!highlight) {
            spinner.background = null
            return
        }
        val d = GradientDrawable()
        d.setStroke(2, 0xFFFF4444.toInt())
        d.cornerRadius = 8f
        spinner.background = d
    }
}


