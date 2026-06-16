package com.ultimatecloner.safe.CloneEngine

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.ultimatecloner.safe.R

class CloneAdapter(
    private val items: List<ClonedApp>,
    private val onClick: (ClonedApp) -> Unit
) : RecyclerView.Adapter<CloneAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardView)
        val name: TextView = view.findViewById(R.id.textAppName)
        val pkg: TextView = view.findViewById(R.id.textPackageName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clone, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.appName
        holder.pkg.text = item.packageName
        holder.card.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
