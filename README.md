# LocationHandler


## Funtion 
### 1. getInstance(Context mContext)
For creating object
### 2. setLocationListener(LocationListener mListener)
Location listener for 
* Location Update 
* Error
* GpsChange
> For Gps change you should call enableGpsChangeCallback nd when application closed don't forget to call disableGpsChangeCallback

### 3. startLocationRequest()
Call this method when you need location update

### 4. stopLocationRequest()
This method stop further location update 

### 5. setLocationRequestInterval(int milliseconds)
This method for automatic location update

### 6. setLocationRequestPriority(int mLocationPriority)
For location update priority

## Use

Add it in your root build.gradle at the end of repositories:

```maven { url 'https://jitpack.io' }```

Add the dependency:

```implementation 'com.github.423u5:LocationHandler:v1.0.0'```
