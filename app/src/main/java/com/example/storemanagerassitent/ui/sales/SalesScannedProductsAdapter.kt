package com.example.storemanagerassitent.ui.sales

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.storemanagerassitent.R

class SalesScannedProductsAdapter(
    private val data: MutableList<SalesBatchScanDialogFragment.ScannedItem>,
    private val onDataChanged: () -> Unit
) : RecyclerView.Adapter<SalesScannedProductsAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_scanned_product, parent, false)
        // 该布局包含名称、规格、数量调节器，无分类 Spinner。若存在 Spinner，将在 onBind 隐藏。
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = data[position]
        holder.tvName.text = item.name
        holder.tvSpec.text = item.spec
        holder.tvQuantity.text = item.quantity.toString()

        // 隐藏分类 Spinner（销售不需要）
        holder.spinner?.visibility = View.GONE

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
                android.app.AlertDialog.Builder(holder.itemView.context)
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
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val btnIncrease: ImageButton = itemView.findViewById(R.id.btnIncrease)
        val btnDecrease: ImageButton = itemView.findViewById(R.id.btnDecrease)
        val spinner: View? = itemView.findViewById(R.id.spinnerCategory)
    }
}




