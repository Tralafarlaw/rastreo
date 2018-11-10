package tralafarlaw.miguel.find;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

public class mapaosm extends AppCompatActivity {

    LocationManager locationManager;
    MapView map;
    Context ctx;
    IMapController mapDriver;
    Location yo;

    //database firbase
    private DatabaseReference databaseReference;

    public void setLocation(Location loc){
        this.yo = loc;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_mapaosm);
        TextView tv =(TextView) findViewById(R.id.Nombre);
        tv.setText(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        init_mapa();
        //empezamos con firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        FirebaseUser fbuser = FirebaseAuth.getInstance().getCurrentUser();
        User user1 = new User(fbuser.getEmail(),yo.getLongitude(),yo.getLatitude(),true,"pnaranja");







    }
    public void init_mapa(){
        map = (MapView) findViewById(R.id.mapaOSM);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        mapDriver = map.getController();


        ActivityCompat.requestPermissions(mapaosm.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(getApplicationContext(),"No se dieron Permisos",Toast.LENGTH_SHORT).show();
        }


        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        MyLocationListener mlocListener = new MyLocationListener();
        mlocListener.setMainActivity(this);
        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,(LocationListener) mlocListener);

        //locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        yo = mlocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);


        Toast.makeText(getApplicationContext(),""+String.valueOf(yo.getLatitude())+" \n"+String.valueOf(yo.getLongitude()), Toast.LENGTH_SHORT).show();

        GeoPoint starPoint = new GeoPoint(yo.getLatitude(),yo.getLongitude());

        mapDriver.setCenter(starPoint);
        mapDriver.setZoom(15.0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        marcadores();
    }

    public void marcadores (){
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<OverlayItem> anotherOverlayItemArray;
                anotherOverlayItemArray = new ArrayList<OverlayItem>();

                ItemizedIconOverlay.OnItemGestureListener<OverlayItem> gestlis = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

                    @Override
                    public boolean onItemLongPress(int arg0, OverlayItem arg1) {
                        // TODO Auto-generated method stub
                        return false;
                    }

                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem item) {
                        Toast.makeText(
                                getApplicationContext(),
                                item.getSnippet() + "\n" + item.getTitle() + "\n"
                                        + item.getPoint().getLatitude() + " : "
                                        + item.getPoint().getLongitude(),
                                Toast.LENGTH_LONG).show();
                        return true;
                    }

                };
                for (DataSnapshot data: dataSnapshot.getChildren()){
                    User user = data.getValue(User.class);
                    Marker mk  = new Marker(map);
                    mk.setIcon(getResources().getDrawable(R.drawable.pppnaranja));
                    mk.setTitle(user.getEmail());
                    mk.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    mk.setPosition(new GeoPoint(user.getLat(),user.getLon()));
                    mk.setVisible(user.isVisible());
                    map.postInvalidate();

                    boolean sw = false;
                    for (Overlay o : map.getOverlays()){
                        Marker aux = (Marker) o ;
                        if(aux.getTitle().equals(mk.getTitle())){
                            Polyline line = new Polyline();
                            List<GeoPoint> v = new ArrayList<>();
                            v.add(aux.getPosition());
                            v.add(mk.getPosition());
                            line.setPoints(v);


                            try {
                                animar(aux, mk);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }


                            sw = true;
                        }
                    }
                    if(!sw){

                        map.getOverlays().add(mk);
                    }
                    //map.getController().animateTo((IGeoPoint) mk.getPosition(), 20.4, 5);
                  //  anotherOverlayItemArray.add(new OverlayItem(user.getEmail(),"",new GeoPoint(user.getLat(),user.getLon())));
                }
        //        ItemizedIconOverlay<OverlayItem> overlay = new ItemizedIconOverlay<>(getApplicationContext(),anotherOverlayItemArray, gestlis);
      //          map.getOverlays().add(overlay);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
     public void animar (final Marker a, Marker b) throws InterruptedException {
        final double latf = b.getPosition().getLatitude();
        final double lonf = b.getPosition().getLongitude();


         if(a.getPosition().equals(b.getPosition())){
             return;
         }
        final double distLat , distLon , constLat, constLon ;

        distLat = Math.abs(a.getPosition().getLatitude()-b.getPosition().getLatitude());
        distLon = Math.abs(a.getPosition().getLongitude()-b.getPosition().getLongitude());

        constLat = distLat/1000;
        constLon = distLon/1000;
        if(a.getTitle().equals("test")){
        Toast.makeText(getApplicationContext(),constLat+" "+constLon,Toast.LENGTH_SHORT).show();}
        long delay = 10;
        final GeoPoint point = a.getPosition();
         for (int i = 0; i < 1000; i++) {
             //Thread.sleep(delay);
             double e=point.getLatitude()+constLat;
             double r=point.getLongitude()+constLon;
             point.setLatitude(e);
             point.setLongitude(r);
             a.setPosition(new GeoPoint(e,r));


             //a.setPosition(b.getPosition());
            // Toast.makeText(getApplicationContext(),"index: "+ i+" \nconstlat = "+constLat+"\ndistlat = "+distLat+"\ne = "+point.getLatitude()+constLat+"\nb = "+point.getLongitude()+constLon,Toast.LENGTH_SHORT).show();
             if(a.getPosition().equals(b.getPosition())){
                 return;
             }
         }
         a.setPosition(new GeoPoint(latf, lonf));
     }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
