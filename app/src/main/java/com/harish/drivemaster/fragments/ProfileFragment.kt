package com.harish.drivemaster.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import com.harish.drivemaster.R

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var gridLayout: GridLayout
    private lateinit var settingsIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_profile, container, false)
        gridLayout = v.findViewById(R.id.gridLayout)
        settingsIcon = v.findViewById(R.id.settingsIcon)

        settingsIcon.setOnClickListener {
            
        }

        populateGrid()

        return v;
    }

    private fun populateGrid() {
        for (i in 0 until 16) { // 4x4 grid
            val inflater = LayoutInflater.from(requireContext())
            val itemView = inflater.inflate(R.layout.grid_item, gridLayout, false)
            val itemText = itemView.findViewById<TextView>(R.id.itemText)

            itemText.text = "Item ${i + 1}" // Set your item text here

            // Set layout parameters for positioning in GridLayout
            val layoutParams = GridLayout.LayoutParams().apply {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                width = 0 // Match parent column width
                height = 0 // Match parent row height
                setMargins(4, 4, 4, 4) // Margin between items
            }
            itemView.layoutParams = layoutParams

            gridLayout.addView(itemView)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}