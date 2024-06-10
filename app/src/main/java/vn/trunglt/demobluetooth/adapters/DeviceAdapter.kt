package vn.trunglt.demobluetooth.adapters

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vn.trunglt.demobluetooth.databinding.ItemBluetoothDeviceBinding

class DeviceAdapter(
    val onItemClicked: (BluetoothDevice) -> Unit
) : ListAdapter<BluetoothDevice, DeviceAdapter.FoundedDeviceVH>(object :
    DiffUtil.ItemCallback<BluetoothDevice>() {
    override fun areItemsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
        return oldItem.address == newItem.address
    }

    override fun areContentsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
        return oldItem == newItem
    }
}) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoundedDeviceVH {
        return FoundedDeviceVH(
            ItemBluetoothDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: FoundedDeviceVH, position: Int) {
        holder.bind(position)
    }

    inner class FoundedDeviceVH(
        private val binding: ItemBluetoothDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.root.text = "${getItem(position).name}===${getItem(position).address}"
            binding.root.setOnClickListener {
                onItemClicked.invoke(getItem(position))
            }
        }
    }
}