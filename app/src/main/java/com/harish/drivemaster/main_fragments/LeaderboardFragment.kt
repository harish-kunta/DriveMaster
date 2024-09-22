package com.harish.drivemaster.main_fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.harish.drivemaster.R
import com.harish.drivemaster.activities.StreakIncreasedActivity
import com.harish.drivemaster.adapters.LeaderboardAdapter
import com.harish.drivemaster.models.FirebaseConstants.Companion.USERS_REF
import com.harish.drivemaster.models.LeaderboardUser

class LeaderboardFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var userLeague: String
    private val userList = mutableListOf<LeaderboardUser>()
    private lateinit var leaderboardAdapter: LeaderboardAdapter
    private lateinit var leagueTitle: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var promotionStatus: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_leaderboard, container, false)
        leagueTitle = view.findViewById(R.id.tvLeaderboardTitle)
        recyclerView = view.findViewById(R.id.recyclerViewLeaderboard)
        leaderboardAdapter = LeaderboardAdapter(userList)
        recyclerView.adapter = leaderboardAdapter

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        fetchUserLeague()

        return view
    }

    private fun fetchUserLeague() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId).child("league")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) {
                    // User has no league assigned, so assign default league
                    val defaultLeague = "ProvisionalDriver"
                    assignUserToLeague(userId, defaultLeague)
                    userLeague = defaultLeague
                } else {
                    // User already has a league assigned
                    userLeague = dataSnapshot.getValue(String::class.java) ?: "ProvisionalDriver"
                }
                leagueTitle.text = userLeague
                fetchLeaderboardData()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }


    private fun assignUserToLeague(userId: String, league: String) {
        // Assign user to the specified league in the Firebase database
        database.child("users").child(userId).child("league").setValue(league)
        database.child("leagues").child(league).child("users").child(userId).setValue(true)
    }

    private fun fetchLeaderboardData() {
        val leagueRef = database.child("leagues").child(userLeague).child("users")

        leagueRef.orderByChild("points").limitToLast(30) // Fetch top 30 players in the league
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    userList.clear()
                    for (snapshot in dataSnapshot.children) {
                        val user = snapshot.getValue(LeaderboardUser::class.java)
                        user?.let { userList.add(it) }
                    }
                    leaderboardAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle error
                }
            })
    }

    // Movement between leagues after every 2 weeks
    private fun handleLeagueMovement() {
        // Call this function at the end of every 2 weeks
        val leagueRef = database.child("leagues").child(userLeague).child("users")

        leagueRef.orderByChild("points").limitToLast(30)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val topPlayers = mutableListOf<String>()
                    val midPlayers = mutableListOf<String>()
                    val bottomPlayers = mutableListOf<String>()

                    val userCount = dataSnapshot.childrenCount.toInt()

                    dataSnapshot.children.forEachIndexed { index, snapshot ->
                        val userId = snapshot.key ?: return@forEachIndexed

                        when {
                            index < userCount - 10 -> bottomPlayers.add(userId)  // Last 10 players
                            index < userCount - 20 -> midPlayers.add(userId)    // Middle 10 players
                            else -> topPlayers.add(userId)                      // Top 10 players
                        }
                    }

                    // Promote top 10 players
                    updateLeague(topPlayers, getHigherLeague(userLeague))

                    // Demote bottom 10 players
                    updateLeague(bottomPlayers, getLowerLeague(userLeague))

                    // Notify users about promotion/demotion
                    if (auth.currentUser?.uid in topPlayers) {
                        promotionStatus = "Promoted"
                        showPromotionDemotionActivity("Promoted")
                    } else if (auth.currentUser?.uid in bottomPlayers) {
                        promotionStatus = "Demoted"
                        showPromotionDemotionActivity("Demoted")
                    } else {
                        promotionStatus = "Same League"
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle error
                }
            })
    }

    // Update user’s league
    private fun updateLeague(users: List<String>, newLeague: String) {
        for (userId in users) {
            // Remove from current league
            database.child("leagues").child(userLeague).child("users").child(userId).removeValue()

            // Add to new league
            database.child("leagues").child(newLeague).child("users").child(userId).setValue(true)

            // Update user league info
            database.child("users").child(userId).child("league").setValue(newLeague)
        }
    }

    private fun getHigherLeague(currentLeague: String): String {
        return when (currentLeague) {
            "ProvisionalDriver" -> "NoviceDriver"
            "NoviceDriver" -> "IntermediateDriver"
            "IntermediateDriver" -> "ExperiencedDriver"
            else -> "ExperiencedDriver"
        }
    }

    private fun getLowerLeague(currentLeague: String): String {
        return when (currentLeague) {
            "ExperiencedDriver" -> "IntermediateDriver"
            "IntermediateDriver" -> "NoviceDriver"
            "NoviceDriver" -> "ProvisionalDriver"
            else -> "ProvisionalDriver"
        }
    }

    private fun showPromotionDemotionActivity(status: String) {
        val intent = Intent(requireContext(), StreakIncreasedActivity::class.java)
        intent.putExtra("status", status)
        startActivity(intent)
    }

    //league levels
//    Learner's Permit League (Entry-level league where everyone starts)
//    Provisional Driver League
//    Intermediate Driver League
//    Experienced Driver League
//    Advanced Driver League
//    Highway Hero League
//    City Cruiser League
//    All-Terrain Champion League
//    Safe Driver League
//    Master Driver League (Top-level league)
}