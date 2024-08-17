package com.harish.drivemaster.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.harish.drivemaster.R
import com.harish.drivemaster.activities.LessonActivity
import com.harish.drivemaster.models.Level

class LevelsAdapter(private val levels: List<Level>) : RecyclerView.Adapter<LevelsAdapter.LevelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_level, parent, false)
        return LevelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        val level = levels[position]
        holder.bind(level)
    }

    override fun getItemCount(): Int = levels.size

    inner class LevelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLevelName: TextView = itemView.findViewById(R.id.tvLevelName)
        private val tvLevelDescription: TextView = itemView.findViewById(R.id.tvLevelDescription)
        private val btnStartLevel: Button = itemView.findViewById(R.id.btnStartLevel)

        fun bind(level: Level) {
            tvLevelName.text = level.name
            tvLevelDescription.text = level.description
            btnStartLevel.isEnabled = !level.isLocked

            btnStartLevel.setOnClickListener {
                if (!level.isLocked) {
                    // Handle level start, e.g., navigate to the lesson activity
                    val intent = Intent(itemView.context, LessonActivity::class.java)
                    intent.putExtra("levelId", level.id)
                    itemView.context.startActivity(intent)
                } else {
                    Toast.makeText(itemView.context, "Level is locked", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
