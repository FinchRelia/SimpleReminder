package com.example.simplereminder

import android.R
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import com.example.simplereminder.databinding.TempListDataBinding
import com.example.simplereminder.db.ReminderData


class ReminderAdaptor(context: Context, private val list: List<ReminderData>) : BaseAdapter() {
    private val inflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, container: ViewGroup?): View? {
        var rowBinding = TempListDataBinding.inflate(inflater, container, false)
        //set payment info values to the list item
        rowBinding.reminderContent.text = list[position].message
        rowBinding.reminderCreation.text = "Created at: " + list[position].creation_time
        rowBinding.reminderDue.text = "Due: " + list[position].reminder_time

        return rowBinding.root
    }

    override fun getItem(position: Int): Any {
        return list[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return list.size
    }
}