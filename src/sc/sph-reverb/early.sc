(pre-include "embree4/rtcore.h" "embree4/rtcore_ray.h"
  "embree4/rtcore_scene.h" "embree4/rtcore_geometry.h")

(define (sp-reverb-early-build-scene geometry materials out-scene)
  (void sp-reverb-early-geometry-t* sp-reverb-early-materials-t* sp-reverb-early-scene-t*)
  (declare
    embree-device RTCDevice
    embree-scene RTCScene
    embree-geometry RTCGeometry
    vertex-list sp-reverb-vec3-t*
    index-list uint32-t*
    vertex-count uint32-t
    index-count uint32-t
    triangle-count uint32-t
    vertex-stride size-t
    index-stride size-t)
  (set
    vertex-list geometry:vertex-list
    index-list geometry:index-list
    vertex-count geometry:vertex-count
    index-count geometry:index-count
    triangle-count (/ index-count (convert-type 3 uint32-t))
    vertex-stride (sizeof sp-reverb-vec3-t)
    index-stride (* (convert-type 3 size-t) (sizeof uint32-t))
    embree-device (rtcNewDevice 0)
    embree-scene (rtcNewScene embree-device)
    embree-geometry (rtcNewGeometry embree-device RTC-GEOMETRY-TYPE-TRIANGLE))
  (rtcSetSharedGeometryBuffer embree-geometry RTC-BUFFER-TYPE-VERTEX
    0 RTC-FORMAT-FLOAT3 vertex-list 0 vertex-stride vertex-count)
  (rtcSetSharedGeometryBuffer embree-geometry RTC-BUFFER-TYPE-INDEX
    0 RTC-FORMAT-UINT3 index-list 0 index-stride triangle-count)
  (rtcCommitGeometry embree-geometry)
  (rtcAttachGeometry embree-scene embree-geometry)
  (rtcReleaseGeometry embree-geometry)
  (rtcCommitScene embree-scene)
  (set
    geometry:embree-scene-handle embree-scene
    out-scene:geometry *geometry
    out-scene:materials *materials
    out-scene:embree-device-handle embree-device
    out-scene:embree-scene-handle embree-scene))

(define (sp-reverb-early-path-compare-by-delay a b) (int (const void*) (const void*))
  (declare pa sp-reverb-early-path-t* pb sp-reverb-early-path-t*)
  (set pa (convert-type a sp-reverb-early-path-t*) pb (convert-type b sp-reverb-early-path-t*))
  (if (< pa:delay pb:delay) (return -1))
  (if (> pa:delay pb:delay) (return 1))
  (return 0))

(define (sp-reverb-early-path-equal a b) (int sp-reverb-early-path-t* sp-reverb-early-path-t*)
  (declare index sp-time-t)
  (if (!= a:delay b:delay) (return 0))
  (if (!= a:gain b:gain) (return 0))
  (if (!= a:direction.x b:direction.x) (return 0))
  (if (!= a:direction.y b:direction.y) (return 0))
  (if (!= a:direction.z b:direction.z) (return 0))
  (if (!= a:order b:order) (return 0))
  (if (!= a:wall-index-count b:wall-index-count) (return 0))
  (set index 0)
  (while (< index a:wall-index-count)
    (if (!= (array-get a:wall-index-chain index) (array-get b:wall-index-chain index)) (return 0))
    (set index (+ index 1)))
  (if (!= a:band-count b:band-count) (return 0))
  (set index 0)
  (while (< index a:band-count)
    (if (!= (array-get a:band-gain-list index) (array-get b:band-gain-list index)) (return 0))
    (set index (+ index 1)))
  (return 1))

(define (sp-reverb-early-paths-union path-set-list path-set-count)
  (sp-reverb-early-path-set-t sp-reverb-early-path-set-t* sp-time-t)
  (declare
    result sp-reverb-early-path-set-t
    set-index sp-time-t
    path-index sp-time-t
    write-index sp-time-t
    unique-count sp-time-t
    count sp-time-t
    path-list sp-reverb-early-path-t*
    set sp-reverb-early-path-set-t*)
  (if (= path-set-count 0) (begin (set result.path-list 0 result.path-count 0) (return result)))
  (set
    result (array-get path-set-list 0)
    path-list result.path-list
    write-index result.path-count
    set-index 1)
  (while (< set-index path-set-count)
    (set set (address-of (array-get path-set-list set-index)) path-index 0)
    (while (< path-index set:path-count)
      (set
        (array-get path-list write-index) (array-get set:path-list path-index)
        write-index (+ write-index 1)
        path-index (+ path-index 1)))
    (set set-index (+ set-index 1)))
  (set result.path-count write-index count result.path-count)
  (if (= count 0) (return result))
  (qsort path-list (convert-type count size-t)
    (sizeof sp-reverb-early-path-t) sp-reverb-early-path-compare-by-delay)
  (set unique-count 1 write-index 1)
  (while (< write-index count)
    (if
      (not
        (sp-reverb-early-path-equal (address-of (array-get path-list (- write-index 1)))
          (address-of (array-get path-list write-index))))
      (begin
        (if (!= unique-count write-index)
          (set (array-get path-list unique-count) (array-get path-list write-index)))
        (set unique-count (+ unique-count 1))))
    (set write-index (+ write-index 1)))
  (set result.path-count unique-count)
  (return result))

(define (sp-reverb-early-direct-path scene source receiver out-path)
  (int sp-reverb-early-scene-t* sp-reverb-early-source-t* sp-reverb-early-receiver-t* sp-reverb-early-path-t*)
  (declare
    embree-scene RTCScene
    ray-hit (struct RTCRayHit)
    args (struct RTCIntersectArguments)
    source-position sp-reverb-vec3-t
    receiver-position sp-reverb-vec3-t
    dx sp-sample-t
    dy sp-sample-t
    dz sp-sample-t
    distance-m sp-sample-t
    inv-distance-m sp-sample-t
    delay-samples sp-sample-t)
  (set
    source-position source:position-world
    receiver-position receiver:position-world
    dx (- receiver-position.x source-position.x)
    dy (- receiver-position.y source-position.y)
    dz (- receiver-position.z source-position.z)
    distance-m (sqrt (+ (* dx dx) (* dy dy) (* dz dz))))
  (if (<= distance-m 0.0) (return 0))
  (set inv-distance-m (/ 1.0 distance-m) delay-samples (* distance-m sp-reverb-sound-meter-sample))
  (memset &ray-hit 0 (sizeof (sc-insert "struct RTCRayHit")))
  (rtcInitIntersectArguments &args)
  (set
    ray-hit.ray.org_x source-position.x
    ray-hit.ray.org_y source-position.y
    ray-hit.ray.org_z source-position.z
    ray-hit.ray.dir_x (convert-type (* dx inv-distance-m) float)
    ray-hit.ray.dir_y (convert-type (* dy inv-distance-m) float)
    ray-hit.ray.dir_z (convert-type (* dz inv-distance-m) float)
    ray-hit.ray.tnear 0.0f
    ray-hit.ray.tfar (convert-type distance-m float)
    ray-hit.ray.time 0.0f
    ray-hit.ray.mask 0xffffffffu
    ray-hit.ray.flags 0
    ray-hit.hit.geomID RTC_INVALID_GEOMETRY_ID
    ray-hit.hit.primID RTC_INVALID_GEOMETRY_ID
    (array-get ray-hit.hit.instID 0) RTC_INVALID_GEOMETRY_ID
    embree-scene (convert-type scene:embree-scene-handle RTCScene))
  (rtcIntersect1 embree-scene &ray-hit &args)
  (if (!= ray-hit.hit.geomID RTC_INVALID_GEOMETRY_ID) (return 0))
  (set
    out-path:delay (convert-type delay-samples sp-time-t)
    out-path:gain inv-distance-m
    out-path:direction.x (* dx inv-distance-m)
    out-path:direction.y (* dy inv-distance-m)
    out-path:direction.z (* dz inv-distance-m)
    out-path:wall-index-chain 0
    out-path:wall-index-count 0
    out-path:order 0
    out-path:band-gain-list 0
    out-path:band-count 0)
  (return 1))

(define (sp-reverb-early-paths-image scene source receiver max-order path-capacity out-path-list)
  (sp-time-t sp-reverb-early-scene-t* sp-reverb-early-source-t* sp-reverb-early-receiver-t* sp-time-t sp-time-t sp-reverb-early-path-t*)
  (declare path sp-reverb-early-path-t count sp-time-t has-direct-path int)
  (set count 0)
  (if (= path-capacity 0) (return 0))
  (set has-direct-path (sp-reverb-early-direct-path scene source receiver &path))
  (if (not has-direct-path) (return 0))
  (set (array-get out-path-list 0) path count 1)
  (return count))

(define
  (sp-reverb-early-paths-beam scene source receiver max-order path-cap portal-index-list portal-index-count)
  (sp-reverb-early-path-set-t sp-reverb-early-scene-t* sp-reverb-early-source-t* sp-reverb-early-receiver-t* sp-time-t sp-time-t uint32-t* uint32-t))

(define
  (sp-reverb-early-paths-diffraction scene edge-index-list edge-index-count band-period-list band-count)
  (sp-reverb-early-path-set-t sp-reverb-early-scene-t* uint32-t* uint32-t sp-time-t* sp-time-t))

(define (sp-reverb-early-paths-cull path-set threshold max-paths)
  (sp-reverb-early-path-set-t sp-reverb-early-path-set-t* sp-sample-t sp-time-t))

(define
  (sp-reverb-early-noise-partials-from-paths path-set layout band-period-start band-period-end duration out-partial-list out-partial-capacity out-partial-count)
  (void sp-reverb-early-path-set-t* sp-reverb-layout-t* sp-time-t sp-time-t sp-time-t sp-reverb-early-noise-partial-t* sp-time-t sp-time-t*))

(define
  (sp-reverb-early-partials-from-paths path-set layout partial-list partial-count out-partial-list out-partial-capacity out-partial-count)
  (void sp-reverb-early-path-set-t* sp-reverb-layout-t* sp-reverb-sampled-partial-t* sp-time-t sp-reverb-early-partial-t* sp-time-t sp-time-t*))

(define
  (sp-reverb-early scene source receiver layout partial-list partial-count out-partial-list out-partial-capacity out-partial-count)
  (void sp-reverb-early-scene-t* sp-reverb-early-source-t* sp-reverb-early-receiver-t* sp-reverb-layout-t* sp-reverb-sampled-partial-t* sp-time-t sp-reverb-early-partial-t* sp-time-t sp-time-t*))
