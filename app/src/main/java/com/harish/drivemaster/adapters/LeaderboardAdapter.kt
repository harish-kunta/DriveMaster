package com.harish.drivemaster.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.harish.drivemaster.R
import com.harish.drivemaster.models.LeaderboardUser

class LeaderboardAdapter(private val userList: List<LeaderboardUser>) :
    RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    class LeaderboardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvUserPoints: TextView = view.findViewById(R.id.tvUserPoints)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_leaderboard_user, parent, false)
        return LeaderboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val user = userList[position]
        holder.tvRank.text = (position + 1).toString() // Rank starts from 1
        holder.tvUserName.text = user.userName
        holder.tvUserPoints.text = "${user.points} Points"
    }

    override fun getItemCount(): Int = userList.size
}

