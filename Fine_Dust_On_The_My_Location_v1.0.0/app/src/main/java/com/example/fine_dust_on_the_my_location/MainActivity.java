package com.example.fine_dust_on_the_my_location;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private NaverMap mMap;
    LatLng curr_LOC;
    Marker marker = null;
    InfoWindow infoWindow1;

    LocationManager locationManager;
    LocationListener locationListener;
    Geocoder geocoder;
    String addr = null;

    DownloadWebpageTask task;
    DownloadWebpageTask2 task2;

    String fine_dust_REAL="real";
    String fine_dust_PUBLIC="public";

    String url = "http://192.168.43.8:8080/";
    String api = "http://openapi.airkorea.or.kr/openapi/services/rest/ArpltnInforInqireSvc/getCtprvnMesureLIst?serviceKey=yRpt6EwKQGHjBfH5b4fN%2BpO77pIjbq5t5QGfFUXisGr61XNCDRKpYWgpdnFX3lzGfmg4z4UuTKMmabLm7gAmfA%3D%3D&numOfRows=1&pageNo=1&itemCode=PM10&dataGubun=HOUR&searchCondition=MONTH";

    String tempLocal = "";
    int localIndex = 0;
    String [] local_eng = {"seoul", "busan", "daegu", "incheon", "gwangju", "daejeon",
            "ulsan", "gyeonggi", "gangwon", "chungbuk", "chungnam", "jeonbuk",
            "jeonnam", "gyeongbuk", "gyeongnam", "jeju", "sejong"};
    String [] local_kor = {"서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시", "대전광역시",
            "울산광역시", "경기도", "강원도", "충청북도", "충청남도", "전라북도",
            "전라남도", "경상북도", "경상남도", "제주특별자치도", "세종특별자치시"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 화면꺼짐방지
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 지도를 출력할 프레그먼트 영역 인식
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.map, mapFragment).commit();
        }

        // 지도 사용이 준비되면 onMapReady 콜백 함수 호출
        mapFragment.getMapAsync(this);
    }

    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        // 지도 객체를 여러 메소도에서 사용할 수 있도록 글로벌 객체로 할당
        mMap = naverMap;

        locationListener = new LocationListener() {
            // 위치가 변할 때마다 호출
            public void onLocationChanged(Location location) {
                updateMap(location);
            }

            // 위치서비스가 변경될 때
            public void onStatusChanged(String provider, int status, Bundle extras) {
                alertStatus(provider);
            }

            // 사용자에 의해 Provider 가 사용 가능하게 설정될 때
            public void onProviderEnabled(String provider) {
                alertProvider(provider);
            }

            // 사용자에 의해 Provider 가 사용 불가능하게 설정될 때
            public void onProviderDisabled(String provider) {
                checkProvider(provider);
            }
        };

        // 시스템 위치 서비스 관리 객체 생성
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // 정확한 위치 접근 권한이 설정되어 있지 않으면 사용자에게 권한 요구
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        String locationProvider;
        // GPS 에 의한 위치 변경 요구
        locationProvider = LocationManager.GPS_PROVIDER;
        locationManager.requestLocationUpdates(locationProvider, 1, 1, locationListener);
        // 통신사 기지국에 의한 위치 변경 요구
        locationProvider = LocationManager.NETWORK_PROVIDER;
        locationManager.requestLocationUpdates(locationProvider, 1, 1, locationListener);

        Log.d("test", "지도 출력"); //-------------------------------------------


        // 미세먼지값 다운로드------------------------------------
        task = new DownloadWebpageTask();
        task.execute(url);

        // 대기오염 정보 API------------------------------------
        task2 = new DownloadWebpageTask2();
        task2.execute(api);

    }

    public void checkProvider(String provider) {
        Toast.makeText(this, provider + "에 의한 위치서비스가 꺼져 있습니다. 켜주세요...", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    public void alertProvider(String provider) {
        Toast.makeText(this, provider + "서비스가 켜졌습니다!", Toast.LENGTH_LONG).show();
    }

    public void alertStatus(String provider) {
        Toast.makeText(this, "위치서비스가 " + provider + "로 변경되었습니다!", Toast.LENGTH_LONG).show();
    }

    public void updateMap(Location location) {

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        curr_LOC = new LatLng(latitude, longitude);

        // 센서값 다운로드----------------------------
//        task.downloadUrl(url);
//        Log.d("test", "센서값 갱신"); //-------------------------------------------


        // 좌표-주소 변환
        geocoder = new Geocoder(this);

        // 지도 중심
        CameraUpdate cameraUpdate1 = CameraUpdate.scrollTo(curr_LOC);
        mMap.moveCamera(cameraUpdate1);

        // 지도 크기
        CameraUpdate cameraUpdate2 = CameraUpdate.zoomTo(15);
        mMap.moveCamera(cameraUpdate2);

        // 미세먼지 다운로드------------------------------------
        DownloadWebpageTask task3 = new DownloadWebpageTask();
        task3.execute(url);
        DownloadWebpageTask2 task4 = new DownloadWebpageTask2();
        task4.execute(api);

        try {
            // 주소 가져오기
            String addr_temp1 = geocoder.getFromLocation(latitude, longitude, 10).toString();
            // 주소 분리1
            String [] addr_temp2 = addr_temp1.split(",");
            // 주소 분리2
            String [] addr_temp3 = addr_temp2[0].split("\"");

            // 주소 추가
            addr = addr_temp3[1];
            String [] tempStr = addr.split(" ");
            tempLocal = tempStr[1];
            Log.d("address", tempLocal); //-------------------------------------------

            for(int i=0; i<local_kor.length; i++) {
                if(tempLocal.equals(local_kor[i])) {
                    localIndex = i;
                    Log.d("address", local_eng[i]); //-------------------------------------------
                }
                else continue;
            }

            // 지도 위에 정보창 출력-------------------------------------------
            if(marker != null)
                marker.setMap(null);
            addInfo(curr_LOC, addr, fine_dust_REAL, fine_dust_PUBLIC);
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.e("error", e.toString());
        }

        Log.d("test", "지도 갱신"); //-------------------------------------------

    }

    // 지도 위에 정보창 표시
    public void addInfo(LatLng latlng, String addr, String data, String data2) {

        final String VALUE = "Real Data: " + data + ", Public Data: " + data2;

        marker = new Marker();
        marker.setPosition(latlng);
        marker.setMap(mMap);

        marker.setSubCaptionText(addr);
        marker.setSubCaptionColor(Color.RED);
        marker.setSubCaptionTextSize(15);

        infoWindow1 = new InfoWindow();
        infoWindow1.setAdapter(new InfoWindow.DefaultTextAdapter(this) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                return VALUE;
            }
        });
        infoWindow1.open(marker);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null)
            locationManager.removeUpdates(locationListener);
    }


    // 미세먼지센서값 데이터 다운로드 클래스 정의--------------------------------------------------------
    private class DownloadWebpageTask extends AsyncTask<String, Void, String> {

        // 문서 다운로드(백그라운드 실행)
        @Override
        protected String doInBackground(String... urls) {

            // 문서 다운로드
            try {
                String txt =  (String) downloadUrl((String) urls[0]);
                return txt;
            } catch (Exception e) {
                Log.e("error", e.toString());
                return e.toString();
            }
        }

        // 문서 다운로드 후 자동 호출
        protected void onPostExecute(String result) {

            Log.d("test", result);//--------------------------------

            String [] splitData1 = result.split(">");
            String tempData = splitData1[1];
            String [] splitData2 = tempData.split("<");
            String finedust_real = splitData2[0];

            Log.d("test", finedust_real);//--------------------------------

            fine_dust_REAL = finedust_real;

        }

        // 전달받은 API에 해당하는 문서 다운로드
        private String downloadUrl(String api) {

            Log.d("test", "다운로드 시작 전"); //-------------------------------------------

            HttpURLConnection conn = null;
            try {

                Log.d("test", "다운로드 시작"); //-------------------------------------------

                // 문서를 읽어 텍스트 단위로 버퍼에 저장
                URL url = new URL(api);
                conn = (HttpURLConnection) url.openConnection();
                Log.d("test", "연결 완료"); //-------------------------------------------
                BufferedInputStream buf = new BufferedInputStream(conn.getInputStream());
                Log.d("test", "바이트데이터 수신 완료"); //-------------------------------------------
                BufferedReader bufreader = new BufferedReader(new InputStreamReader(buf, "utf-8"));
                Log.d("test", "텍스트데이터 수신 완료"); //-------------------------------------------

                // 줄 단위로 읽어 문자로 저장
                String line = null;
                String page = "";
                while ((line = bufreader.readLine()) != null) {
                    page += line;
                }

                // 다운로드 문서 반환
                return page;

            }
            catch(Exception e) {
                Log.d("test", e.toString()); //-------------------------------------------
                return e.toString();
            }

            finally
            {
                conn.disconnect();
                Log.d("test", "연결종료"); //-------------------------------------------
            }
        }
    }


    // 미세먼지센서값 데이터 다운로드 클래스 정의--------------------------------------------------------
    private class DownloadWebpageTask2 extends AsyncTask<String, Void, String> {

        // 문서 다운로드(백그라운드 실행)
        @Override
        protected String doInBackground(String... urls) {

            // 문서 다운로드
            try {
                String txt =  (String) downloadUrl((String) urls[0]);
                return txt;
            } catch (Exception e) {
                Log.e("error", e.toString());
                return e.toString();
            }

        }

        // 문서 다운로드 후 자동 호출
        protected void onPostExecute(String result) {

            Log.d("test", result);//--------------------------------

            boolean bSet_itemCode = false;
            boolean bSet_city = false;

            String itemCode = "";
            String pollution_degree = "";
            String tag_name = "";

            try {
                // XML Pull Parser 객체 생성
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();

                // 파싱할 문서 설정
                xpp.setInput(new StringReader(result));

                // 현재 이벤트 유형 반환(START_DOCUMENT, START_TAG, TEXT, END_TAG, END_DOCUMENT
                int eventType = xpp.getEventType();

                // 이벤트 유형이 문서 마지막이 될 때까지 반복
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    // 문서의 시작인 경우
                    if (eventType == XmlPullParser.START_DOCUMENT) {
                        // START_TAG이면 태그 이름 확인
                    } else if (eventType == XmlPullParser.START_TAG) {

                        tag_name = xpp.getName();
                        if (bSet_itemCode == false && tag_name.equals("itemCode"))
                            bSet_itemCode = true;
                        if (itemCode.equals("PM10") && (tag_name.equals(local_eng[localIndex])))
                            bSet_city = true;

                        // 태그 사이의 문자 확인
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (bSet_itemCode) {
                            itemCode = xpp.getText();

                            if (itemCode.equals("PM10")) {
                                bSet_itemCode = false;
                            }
                        }
                        if (bSet_city) {
                            pollution_degree = xpp.getText();

                            // 도시와 미세먼지 농도 화면 출력--------------------------------------------------
                            fine_dust_PUBLIC = pollution_degree;
                            Log.d("test", fine_dust_PUBLIC);//---------------------------------------------

                            bSet_city = false;
                        }

                        // 마침 태그인 경우
                    } else if (eventType == XmlPullParser.END_TAG) {  }

                    // 다음 이벤트 유형 할당
                    eventType = xpp.next();
                }
            } catch (Exception e) {
                Log.e("error", e.toString());
            }

        }

        // 전달받은 API에 해당하는 문서 다운로드
        private String downloadUrl(String api) {

            Log.d("test", "다운로드 시작 전2"); //-------------------------------------------

            HttpURLConnection conn = null;
            try {

                Log.d("test", "다운로드 시작2"); //-------------------------------------------

                // 문서를 읽어 텍스트 단위로 버퍼에 저장
                URL url = new URL(api);
                conn = (HttpURLConnection) url.openConnection();
                Log.d("test", "연결 완료2"); //-------------------------------------------
                BufferedInputStream buf = new BufferedInputStream(conn.getInputStream());
                Log.d("test", "바이트데이터 수신 완료2"); //-------------------------------------------
                BufferedReader bufreader = new BufferedReader(new InputStreamReader(buf, "utf-8"));
                Log.d("test", "텍스트데이터 수신 완료2"); //-------------------------------------------

                // 줄 단위로 읽어 문자로 저장
                String line = null;
                String page = "";
                while ((line = bufreader.readLine()) != null) {
                    page += line;
                }

                Log.d("test", "다운로드 완료2"); //-------------------------------------------

                // 다운로드 문서 반환
                return page;

            }
            catch(Exception e) {
                Log.d("test", e.toString()); //-------------------------------------------
                return e.toString();
            }

            finally
            {
                conn.disconnect();
                Log.d("test", "연결종료2"); //-------------------------------------------
            }
        }

    }

}
