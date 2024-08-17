package com.harish.drivemaster.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.harish.drivemaster.R
import com.harish.drivemaster.adapters.LevelsAdapter
import com.harish.drivemaster.models.Level

// LearnFragment.kt
class LearnFragment : Fragment() {

    private lateinit var levelsRecyclerView: RecyclerView
    private lateinit var levelsAdapter: LevelsAdapter
    private val levelsList = mutableListOf<Level>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_learn, container, false)

        levelsRecyclerView = view.findViewById(R.id.recyclerViewLevels)
        levelsAdapter = LevelsAdapter(levelsList)
        levelsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        levelsRecyclerView.adapter = levelsAdapter

        fetchLevels()

        return view
    }

    private fun fetchLevels() {
        val levelsRef = FirebaseDatabase.getInstance().getReference("lessons")
        levelsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                levelsList.clear()
                for (snapshot in dataSnapshot.children) {
                    val level = snapshot.getValue(Level::class.java)
                    level?.let { levelsList.add(it) }
                }
                levelsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load levels.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
