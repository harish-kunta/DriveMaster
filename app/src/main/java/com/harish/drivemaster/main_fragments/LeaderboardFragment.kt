package com.harish.drivemaster.main_fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.harish.drivemaster.R
import com.harish.drivemaster.adapters.LeaderboardAdapter
import com.harish.drivemaster.models.FirebaseConstants.Companion.USERS_REF
import com.harish.drivemaster.models.LeaderboardUser

class LeaderboardFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LeaderboardAdapter
    private lateinit var database: DatabaseReference
    private var leaderboardUsers = mutableListOf<LeaderboardUser>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_leaderboard, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewLeaderboard)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = LeaderboardAdapter(leaderboardUsers)
        recyclerView.adapter = adapter

        database = FirebaseDatabase.getInstance().reference

        fetchLeaderboardData()

        return view
    }

    private fun fetchLeaderboardData() {
        // Reference to users in the Firebase database
        val usersRef = database.child(USERS_REF)

        usersRef.orderByChild("points").limitToLast(10)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    leaderboardUsers.clear()
                    for (userSnapshot in snapshot.children) {
                        val userId = userSnapshot.key ?: continue
                        val userName =
                            userSnapshot.child("userName").getValue(String::class.java) ?: "Unknown"
                        val points = userSnapshot.child("points").getValue(Int::class.java) ?: 0
                        val streak = userSnapshot.child("streak").child("currentStreak")
                            .getValue(Int::class.java) ?: 0

                        val leaderboardUser = LeaderboardUser(userId, userName, points, streak)
                        leaderboardUsers.add(leaderboardUser)
                    }

                    leaderboardUsers.sortByDescending { it.points } // Sort users by points
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("LeaderboardFragment", "Failed to fetch data", error.toException())
                }
            })
    }
}