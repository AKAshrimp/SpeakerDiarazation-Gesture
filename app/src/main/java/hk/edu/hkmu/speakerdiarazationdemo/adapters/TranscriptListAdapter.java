package hk.edu.hkmu.speakerdiarazationdemo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import hk.edu.hkmu.speakerdiarazationdemo.R;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TranscriptListAdapter extends RecyclerView.Adapter<TranscriptListAdapter.ViewHolder> {

    public interface Callback {
        void onViewTranscript(File file);
        void onDeleteTranscript(File file);
        void onRenameTranscript(File file);
    }

    private final List<File> items = new ArrayList<>();
    private final DecimalFormat sizeFormatter = new DecimalFormat("#,##0.0");
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private final Callback callback;

    public TranscriptListAdapter(Callback callback) {
        this.callback = callback;
    }

    public void setItems(List<File> files) {
        items.clear();
        if (files != null) {
            items.addAll(files);
        }
        notifyDataSetChanged();
    }

    public void appendItems(List<File> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        int start = items.size();
        items.addAll(files);
        notifyItemRangeInserted(start, files.size());
    }

    public File getItem(int position) {
        return items.get(position);
    }

    public void removeItem(File file) {
        int index = items.indexOf(file);
        if (index >= 0) {
            items.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_audio_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = items.get(position);
        holder.tvName.setText(file.getName());
        double sizeKb = file.length() / 1024.0;
        String info = String.format(
                Locale.getDefault(),
                "大小：%s KB  |  儲存：%s",
                sizeFormatter.format(sizeKb),
                timeFormatter.format(new Date(file.lastModified()))
        );
        holder.tvInfo.setText(info);

        holder.btnView.setOnClickListener(v -> {
            if (callback != null) {
                callback.onViewTranscript(file);
            }
        });
        holder.btnRename.setOnClickListener(v -> {
            if (callback != null) {
                callback.onRenameTranscript(file);
            }
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (callback != null) {
                callback.onDeleteTranscript(file);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvInfo;
        final Button btnView;
        final Button btnRename;
        final Button btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAudioFileName);
            tvInfo = itemView.findViewById(R.id.tvAudioFileInfo);
            btnView = itemView.findViewById(R.id.btnPlayAudioFile);
            btnRename = itemView.findViewById(R.id.btnRenameAudioFile);
            btnDelete = itemView.findViewById(R.id.btnDeleteAudioFile);
        }
    }
}
