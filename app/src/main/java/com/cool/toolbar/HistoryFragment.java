package com.cool.toolbar;

import android.content.Context;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

/**
 * Created by Cool on 12.04.2017.
 */

// фрагмент с историей запросов. по нажатию на кнопки внизу экрана фильтруется либо на все слова, либо тошлько на избранные
public class HistoryFragment extends Fragment {

    protected RecyclerView allRecyclerView;
    protected WordAdapter wordAdapter;
    protected RecyclerView.LayoutManager layoutManager;
    protected List<ItemWordModel> dataSet;
    int scrollPosition = 0;
    protected UpdateUIInterface updater;
    public String filter = "";
    public boolean modeShowAllWords;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // получаю набор данных для отображения в RecyclerView
        dataSet = Utils.getDataFromDB(filter, modeShowAllWords);
        Log.d("dbg", dataSet.toString());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_history, container, false);
        rootView.setTag("RecyclerViewFragment");
        allRecyclerView = (RecyclerView) rootView.findViewById(R.id.allRecyclerView);
        allRecyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        allRecyclerView.setLayoutManager(layoutManager);
        wordAdapter = new WordAdapter(getActivity(), dataSet);
        allRecyclerView.setAdapter(wordAdapter);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof UpdateUIInterface) {
            updater = (UpdateUIInterface) context;
        }
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        updater = null;
        EventBus.getDefault().unregister(this);
    }

    // обновление RecyclerView в зависимости от фильтра и отображения только избранного
    public void updateUI(String listFilter, boolean showAll) {
        filter = listFilter;
        modeShowAllWords = showAll;
        if (listFilter == null) listFilter = "";
        if (allRecyclerView != null) {
            dataSet = Utils.getDataFromDB(filter, modeShowAllWords);
            int scrollPosition = -1;

            if (allRecyclerView.getLayoutManager() != null) {
                scrollPosition = ((LinearLayoutManager) allRecyclerView.getLayoutManager())
                        .findFirstCompletelyVisibleItemPosition();
            }
            if (wordAdapter != null) {
                wordAdapter.swapData(dataSet);
            }
        }
    }

    @Subscribe
    public void onEvent(EBusMessage msg){
//        Toast.makeText(this, "EBus works", Toast.LENGTH_LONG).show();
        if (msg.id.equals(EBusMessage.EVENTBUS_UPDATE_HISTORY)){

                updateUI(filter, modeShowAllWords);
        }
    }
}
