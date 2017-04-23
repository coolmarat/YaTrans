package com.cool.toolbar;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Cool on 22.04.2017.
 */

// адаптер для RecyclerView
public class WordAdapter extends RecyclerView.Adapter<WordAdapter.WordViewHolder> {

    static List<ItemWordModel> dbList;
    static Context context;

    WordAdapter(Context context, List<ItemWordModel> dbList){
        this.dbList = new ArrayList<ItemWordModel>();
        this.context = context;
        this.dbList = dbList;
    }

    @Override
    public WordAdapter.WordViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.recycler_item, null);

        // создаем ViewHolder
        WordViewHolder wordViewHolder = new WordViewHolder(itemLayoutView);
        return wordViewHolder;
    }

    @Override
    public void onBindViewHolder(WordViewHolder holder, int position) {
        // заполняю данными текущий элемент RecyclerView
        holder.tvTranslatedText.setText(dbList.get(position).getTranslatedWord());;
        holder.tvSrcText.setText(dbList.get(position).getSrcWord());
        holder.tvLngDest.setText(dbList.get(position).getSrcShortLng() + "-" + dbList.get(position).getDestShortLng());
        // иконку прорисовываю в зависимости от "избранности" элемента
        if (dbList.get(position).getIsFavorite() == 1) {
            holder.btnFavorite.setImageResource(R.drawable.ic_star_filled);
        } else {
            holder.btnFavorite.setImageResource(R.drawable.ic_star);
        }
    }

    // замена данных при фильтрации
    public void swapData(List<ItemWordModel> newData){
        dbList.clear();
        dbList.addAll(newData);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return dbList.size();
    }

    public static class WordViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView tvTranslatedText, tvSrcText, tvLngDest;
        public ImageButton btnFavorite, btnDelItem;

        public WordViewHolder(View itemView) {
            super(itemView);
            try {
                Log.d("dbg", itemView.toString());
                tvTranslatedText = (TextView) itemView.findViewById(R.id.tvTranslate);
                tvSrcText = (TextView) itemView.findViewById(R.id.tvSrcText);
                tvLngDest = (TextView) itemView.findViewById(R.id.tvLngDest);
                btnDelItem = (ImageButton) itemView.findViewById(R.id.btnDelItem);
                btnFavorite = (ImageButton) itemView.findViewById(R.id.btnFavoriteItem);

                // здесь назначить обработчики кнопок
                btnDelItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d("dbg", "you have DELETE Row " + getAdapterPosition());
                        ItemWordModel itemWordModel = dbList.get(getAdapterPosition());
                        Utils.deleteWordById(itemWordModel.getId());

                        // посылаю во фрагмент сообщение, что надо обновить интерфейс
                        EventBus.getDefault().post(new EBusMessage(EBusMessage.EVENTBUS_UPDATE_HISTORY, null, null));
                    }
                });

                btnFavorite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d("dbg", "you have FAVORITE Row " + getAdapterPosition());
                        ItemWordModel itemWordModel = dbList.get(getAdapterPosition());
                        int newFavValue = Math.abs(itemWordModel.getIsFavorite() - 1);
                        Utils.changeWordFavorite(itemWordModel.getId(), newFavValue);

                        // посылаю во фрагмент сообщение, что надо обновить интерфейс
                        EventBus.getDefault().post(new EBusMessage(EBusMessage.EVENTBUS_UPDATE_HISTORY, null, null));
                    }
                });

                itemView.setOnClickListener(this);
            } catch (Exception e) {
                Log.d("dbg", "wordViewHolder create");
            }
        }

        @Override
        public void onClick(View v) {
            Log.d("dbg", "you have CLICK Row " + getAdapterPosition());
            // вытаскиваю нажатое слово из базы и делаю обновление интерфейса с переводом
            ItemWordModel itemWordModel = dbList.get(getAdapterPosition());
            TranslateWord tw = Utils.getTranslateFromDB(itemWordModel.getSrcWord(), itemWordModel.getSrcShortLng(), itemWordModel.getDestShortLng());

            EventBus.getDefault().post(new EBusMessage(EBusMessage.EVENTBUS_SHOW_COMPLETE_TRANSLATE, tw, null));
        }
    }
}
