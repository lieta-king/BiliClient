package com.RobinNotBad.BiliClient.activity.video.local;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.InstanceActivity;
import com.RobinNotBad.BiliClient.adapter.video.LocalVideoAdapter;
import com.RobinNotBad.BiliClient.api.ConfInfoApi;
import com.RobinNotBad.BiliClient.model.LocalVideo;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.FileUtil;
import com.RobinNotBad.BiliClient.util.MsgUtil;

import java.io.File;
import java.util.ArrayList;

//本地列表
//2023-08-07

public class LocalListActivity extends InstanceActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private final ArrayList<LocalVideo> videoList = new ArrayList<>(10);
    private LocalVideoAdapter adapter;

    private int longClickPosition=-1;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_main_refresh);
        setMenuClick();

        recyclerView = findViewById(R.id.recyclerView);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        TextView pageName = findViewById(R.id.pageName);
        pageName.setText("缓存");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }

        CenterThreadPool.run(() -> {
            runOnUiThread(()->swipeRefreshLayout.setRefreshing(true));
            scan(ConfInfoApi.getDownloadPath(this));
            adapter = new LocalVideoAdapter(this,videoList);
            adapter.setOnLongClickListener(position -> {
                if(longClickPosition == position) {
                    File file = new File(ConfInfoApi.getDownloadPath(this), videoList.get(position).title);
                    CenterThreadPool.run(()-> FileUtil.deleteFolder(file));
                    MsgUtil.toast("删除成功",this);
                    videoList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position,videoList.size() - position);
                    longClickPosition = -1;
                }
                else{
                    longClickPosition = position;
                    MsgUtil.toast("再次长按删除",this);
                }
            });
            runOnUiThread(()->{
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(adapter);
                swipeRefreshLayout.setRefreshing(false);
            });
        });
    }

    private void scan(File folder){
        File[] files = folder.listFiles();
        if(files!=null) {
            for (File video : files) {
                if (video.isDirectory()) {
                    LocalVideo localVideo = new LocalVideo();
                    localVideo.title = video.getName();
                    localVideo.cover = (new File(video, "cover.png")).toString();

                    localVideo.pageList = new ArrayList<>();
                    localVideo.danmakuFileList = new ArrayList<>();
                    localVideo.videoFileList = new ArrayList<>();

                    File videoFile = new File(video,"video.mp4");
                    File danmakuFile = new File(video,"danmaku.xml");
                    if(videoFile.exists() && danmakuFile.exists()){
                        localVideo.videoFileList.add(videoFile.toString());
                        localVideo.danmakuFileList.add(danmakuFile.toString());    //单集视频
                        videoList.add(localVideo);
                    }
                    else {
                        File[] pages = video.listFiles();      //分页视频
                        if(pages!=null) {
                            for (File page : pages) {
                                if(page.isDirectory()) {
                                    File pageVideoFile = new File(page, "video.mp4");
                                    File pageDanmakuFile = new File(page, "danmaku.xml");
                                    if (pageVideoFile.exists() && pageDanmakuFile.exists()) {
                                        localVideo.pageList.add(page.getName());
                                        localVideo.videoFileList.add(pageVideoFile.toString());
                                        localVideo.danmakuFileList.add(pageDanmakuFile.toString());
                                    }
                                }
                            }
                            videoList.add(localVideo);
                        }
                    }

                }
            }
        }
    }

    public void refresh(){
        CenterThreadPool.run(()->{
            runOnUiThread(()->swipeRefreshLayout.setRefreshing(true));
            int oldSize = videoList.size();
            videoList.clear();
            scan(ConfInfoApi.getDownloadPath(this));
            runOnUiThread(()->{
                adapter.notifyItemRangeChanged(0,oldSize);
                swipeRefreshLayout.setRefreshing(false);
            });
        });
    }
}