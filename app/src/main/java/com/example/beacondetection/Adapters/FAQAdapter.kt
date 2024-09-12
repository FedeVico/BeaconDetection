package com.example.beacondetection.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView
import com.example.beacondetection.R

class FAQAdapter(
    private val context: Context,
    private val questionList: List<String>,
    private val answerList: HashMap<String, List<String>>,
    private val expandableListView: ExpandableListView
) : BaseExpandableListAdapter() {

    override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
        return this.answerList[this.questionList[listPosition]]!![expandedListPosition]
    }

    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }

    override fun getChildrenCount(listPosition: Int): Int {
        return this.answerList[this.questionList[listPosition]]!!.size
    }

    override fun getGroup(listPosition: Int): Any {
        return this.questionList[listPosition]
    }

    override fun getGroupCount(): Int {
        return this.questionList.size
    }

    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
        return true
    }

    override fun getGroupView(listPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        var convertView = convertView
        val questionTitle = getGroup(listPosition) as String
        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.list_group, null)
        }
        val listTitleTextView = convertView!!.findViewById<TextView>(R.id.listTitle)
        listTitleTextView.text = questionTitle

        // Set up click listener to expand or collapse group
        convertView.setOnClickListener {
            if (expandableListView.isGroupExpanded(listPosition)) {
                expandableListView.collapseGroup(listPosition)
            } else {
                // Collapse all other groups
                val groupCount = groupCount
                for (i in 0 until groupCount) {
                    if (i != listPosition) {
                        expandableListView.collapseGroup(i)
                    }
                }
                expandableListView.expandGroup(listPosition, true)
            }
        }

        return convertView
    }

    override fun getChildView(listPosition: Int, expandedListPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
        var convertView = convertView
        val answerText = getChild(listPosition, expandedListPosition) as String
        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.list_item, null)
        }
        val expandedListTextView = convertView!!.findViewById<TextView>(R.id.expandedListItem)
        expandedListTextView.text = answerText
        return convertView
    }
}
