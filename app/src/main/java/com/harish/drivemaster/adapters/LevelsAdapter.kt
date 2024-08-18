package com.harish.drivemaster.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.harish.drivemaster.R
import com.harish.drivemaster.models.Level

class LevelsAdapter(
    private val levels: List<Level>,
    private val onLevelClick: (String) -> Unit
) : RecyclerView.Adapter<LevelsAdapter.LevelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_level, parent, false)
        return LevelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        val level = levels[position]
        holder.bind(level, onLevelClick)
    }

    override fun getItemCount(): Int = levels.size

    inner class LevelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLevelName: TextView = itemView.findViewById(R.id.tvLevelName)
        private val tvLevelDescription: TextView = itemView.findViewById(R.id.tvLevelDescription)
        private val btnStartLevel: Button = itemView.findViewById(R.id.btnStartLevel)

        fun bind(level: Level, onLevelClick: (String) -> Unit) {
            tvLevelName.text = level.name
            tvLevelDescription.text = level.description

            itemView.setOnClickListener {
                onLevelClick(level.id.toString())
            }
        }
    }
}
