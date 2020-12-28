package com.example.whatsappclone;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.Context.MODE_APPEND;


public class ChatsFragment extends Fragment {
    private RecyclerView convList;
    private DatabaseReference convRef,messageRef,usersRef;
    private View mainView;
    SharedPreferences pref;
    String uid;


    public ChatsFragment() {
        // Required empty public constructor
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = this.getActivity().getSharedPreferences("UID",MODE_APPEND);
        uid = pref.getString("phoneNumber","");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_chats, container, false);
        convList = mainView.findViewById(R.id.chatsView);

        convRef = FirebaseDatabase.getInstance().getReference().child("chat").child(uid);
        convRef.keepSynced(true);

        usersRef = FirebaseDatabase.getInstance().getReference().child("users");
        messageRef = FirebaseDatabase.getInstance().getReference().child("messages").child(uid);
        usersRef.keepSynced(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        convList.setHasFixedSize(true);
        convList.setLayoutManager(linearLayoutManager);

        return mainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Query query = convRef.orderByChild("timeStamp");
        query.keepSynced(true);

        final FirebaseRecyclerOptions<ConvClass> options =
                new FirebaseRecyclerOptions.Builder<ConvClass>()
                        .setQuery(query, new SnapshotParser<ConvClass>() {
                            @NonNull
                            @Override
                            public ConvClass parseSnapshot(@NonNull DataSnapshot snapshot) {
                                return new ConvClass((Boolean)snapshot.child("seen").getValue(),snapshot.child("timeStamp").getValue(Long.class));
                            }
                        })
                        .build();

        FirebaseRecyclerAdapter<ConvClass, ConvViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<ConvClass, ConvViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ConvViewHolder holder, final int position, @NonNull final ConvClass model) {
                final String userId = getRef(position).getKey();
                final Query lastMessageQuery = messageRef.child(userId).limitToLast(1);
                lastMessageQuery.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        if(snapshot.exists())
                        { String data = snapshot.child("message").getValue().toString();
                        String type = snapshot.child("type").getValue().toString();
                        if(type.equals("text")){
                            holder.setMessage(data,model.isSeen());
                        }else{
                            holder.setMessage("Image",model.isSeen());
                        }
                    }}

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        notifyItemRemoved(position);
                    }
                });

                usersRef.child(userId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        final String userName = snapshot.child("name").getValue(String.class);
                        final String thumbnailImage = snapshot.child("thumbnail").getValue(String.class);

                        GetTimeAgo getTimeAgo = new GetTimeAgo();
                        long lastSeenTime =  snapshot.child("lastSeen").getValue(Long.class);
                        String lastSeenString = getTimeAgo.getTimeAgo(lastSeenTime);

                        holder.setOnline(lastSeenString);
                        holder.setName(userName);
                        holder.setImage(thumbnailImage);

                        holder.view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent chatIntent = new Intent(getContext(),ChatActivity.class);
                                chatIntent.putExtra("userId",userId);
                                chatIntent.putExtra("UID",uid);
                                chatIntent.putExtra("thumbnail",thumbnailImage);
                                chatIntent.putExtra("name",userName);
                                startActivity(chatIntent);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        notifyItemRemoved(position);
                    }
                });

            }

            @NonNull
            @Override
            public ConvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_single_layout, parent, false);
                return new ConvViewHolder(view);
            }
        };



        convList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    private class ConvViewHolder extends RecyclerView.ViewHolder{
        View view;

        public ConvViewHolder(View itemView){
            super(itemView);
            view = itemView;
        }

        public void setMessage(String message,boolean isSeen){
            TextView userStatusView = view.findViewById(R.id.userSingleStatus);
            userStatusView.setText(message);

            if(!isSeen){
                userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.BOLD);
            }else{
                userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.BOLD);
            }
        }

        public void setImage(final String thumbnailImage){
            final CircleImageView circleImageView = view.findViewById(R.id.userSingleDisplayPicture);
            Picasso.get().load(thumbnailImage).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_dp).into(circleImageView, new Callback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onError(Exception e) {
                    Picasso.get().load(thumbnailImage).placeholder(R.drawable.default_dp).into(circleImageView);
                }
            });
        }

        public void setName(String name){
            TextView userNameView = view.findViewById(R.id.userSingleDisplayName);
            userNameView.setText(name);
        }

        public void setOnline(final String lastSeen){
            final ImageView imageView = view.findViewById(R.id.onlineStatus);
            Picasso.get().load(lastSeen).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_dp).into(imageView, new Callback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onError(Exception e) {
                    Picasso.get().load(lastSeen).placeholder(R.drawable.default_dp).into(imageView);
                }
            });
        }
    }
}