(pre-include "embree4/rtcore.h" "embree4/rtcore_ray.h"
  "embree4/rtcore_scene.h" "embree4/rtcore_geometry.h")

(define (sp-reverb-early-context-init scene out-context)
  (void sp-reverb-scene-t* sp-reverb-early-context-t*)
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
    vertex-list scene:geometry.vertex-list
    index-list scene:geometry.index-list
    vertex-count scene:geometry.vertex-count
    index-count scene:geometry.index-count
    triangle-count (/ index-count (convert-type 3 uint32-t))
    vertex-stride (sizeof sp-reverb-vec3-t)
    index-stride (* (convert-type 3 size-t) (sizeof uint32-t))
    embree-device (rtcNewDevice 0)
    embree-scene (rtcNewScene embree-device)
    embree-geometry (rtcNewGeometry embree-device RTC_GEOMETRY_TYPE_TRIANGLE))
  (rtcSetSharedGeometryBuffer embree-geometry RTC_BUFFER_TYPE_VERTEX
    0 RTC_FORMAT_FLOAT3 vertex-list 0 vertex-stride vertex-count)
  (rtcSetSharedGeometryBuffer embree-geometry RTC_BUFFER_TYPE_INDEX
    0 RTC_FORMAT_UINT3 index-list 0 index-stride triangle-count)
  (rtcCommitGeometry embree-geometry)
  (rtcAttachGeometry embree-scene embree-geometry)
  (rtcReleaseGeometry embree-geometry)
  (rtcCommitScene embree-scene)
  (set
    out-context:scene scene
    out-context:embree-device-handle embree-device
    out-context:embree-scene-handle embree-scene))

(define (sp-reverb-early-context-shutdown context) (void sp-reverb-early-context-t*)
  (declare embree-scene RTCScene embree-device RTCDevice)
  (set
    embree-scene (convert-type context:embree-scene-handle RTCScene)
    embree-device (convert-type context:embree-device-handle RTCDevice))
  (rtcReleaseScene embree-scene)
  (rtcReleaseDevice embree-device)
  (set context:scene 0 context:embree-scene-handle 0 context:embree-device-handle 0))

(define (sp-reverb-early-path-compare-by-delay value-a value-b) (int (const void*) (const void*))
  (declare path-a sp-reverb-early-path-t* path-b sp-reverb-early-path-t*)
  (set
    path-a (convert-type value-a sp-reverb-early-path-t*)
    path-b (convert-type value-b sp-reverb-early-path-t*))
  (if (< path-a:delay path-b:delay) (return -1))
  (if (> path-a:delay path-b:delay) (return 1))
  (return 0))

(define (sp-reverb-early-path-equal path-a path-b)
  (sp-bool-t sp-reverb-early-path-t* sp-reverb-early-path-t*)
  (declare index sp-time-t)
  (if (!= path-a:delay path-b:delay) (return 0))
  (if (!= path-a:direction-receiver.x path-b:direction-receiver.x) (return 0))
  (if (!= path-a:direction-receiver.y path-b:direction-receiver.y) (return 0))
  (if (!= path-a:direction-receiver.z path-b:direction-receiver.z) (return 0))
  (if (!= path-a:direction-source.x path-b:direction-source.x) (return 0))
  (if (!= path-a:direction-source.y path-b:direction-source.y) (return 0))
  (if (!= path-a:direction-source.z path-b:direction-source.z) (return 0))
  (if (!= path-a:order path-b:order) (return 0))
  (if (!= path-a:triangle-index-count path-b:triangle-index-count) (return 0))
  (set index 0)
  (while (< index path-a:triangle-index-count)
    (if
      (!= (array-get path-a:triangle-index-chain index)
        (array-get path-b:triangle-index-chain index))
      (return 0))
    (set index (+ index 1)))
  (if (!= path-a:path-type path-b:path-type) (return 0))
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
    path-set sp-reverb-early-path-set-t*)
  (if (= path-set-count 0) (begin (set result.path-list 0 result.path-count 0) (return result)))
  (set
    result (array-get path-set-list 0)
    path-list result.path-list
    write-index result.path-count
    set-index 1)
  (while (< set-index path-set-count)
    (set path-set (address-of (array-get path-set-list set-index)) path-index 0)
    (while (< path-index path-set:path-count)
      (set
        (array-get path-list write-index) (array-get path-set:path-list path-index)
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

(define (sp-reverb-early-direct-path context source receiver cutoff out-path)
  (sp-bool-t sp-reverb-early-context-t* sp-reverb-early-source-t* sp-reverb-early-receiver-t* sp-reverb-early-cutoff-t* sp-reverb-early-path-t*)
  (declare
    embree-scene RTCScene
    ray-hit (struct RTCRayHit)
    intersect-arguments (struct RTCIntersectArguments)
    source-position sp-reverb-vec3-t
    receiver-position sp-reverb-vec3-t
    delta-x sp-sample-t
    delta-y sp-sample-t
    delta-z sp-sample-t
    distance-meter sp-sample-t
    inverse-distance-meter sp-sample-t
    delay-sample sp-sample-t)
  (set
    source-position source:position-world
    receiver-position receiver:position-world
    delta-x (- receiver-position.x source-position.x)
    delta-y (- receiver-position.y source-position.y)
    delta-z (- receiver-position.z source-position.z)
    distance-meter (sqrt (+ (* delta-x delta-x) (* delta-y delta-y) (* delta-z delta-z))))
  (if (<= distance-meter 0.0) (return 0))
  (set
    inverse-distance-meter (/ 1.0 distance-meter)
    delay-sample (* distance-meter sp-reverb-sound-meter-sample))
  (if cutoff
    (begin
      (if (> delay-sample (convert-type cutoff:max-delay sp-sample-t)) (return 0))
      (if (> distance-meter cutoff:max-path-length) (return 0))))
  (memset &ray-hit 0 (sizeof (sc-insert "struct RTCRayHit")))
  (rtcInitIntersectArguments &intersect-arguments)
  (set
    ray-hit.ray.org-x source-position.x
    ray-hit.ray.org-y source-position.y
    ray-hit.ray.org-z source-position.z
    ray-hit.ray.dir-x (convert-type (* delta-x inverse-distance-meter) float)
    ray-hit.ray.dir-y (convert-type (* delta-y inverse-distance-meter) float)
    ray-hit.ray.dir-z (convert-type (* delta-z inverse-distance-meter) float)
    ray-hit.ray.tnear 0.0f
    ray-hit.ray.tfar (convert-type distance-meter float)
    ray-hit.ray.time 0.0f
    ray-hit.ray.mask 0xffffffffu
    ray-hit.ray.flags 0
    ray-hit.hit.geomID RTC_INVALID_GEOMETRY_ID
    ray-hit.hit.primID RTC_INVALID_GEOMETRY_ID
    (array-get ray-hit.hit.instID 0) RTC_INVALID_GEOMETRY_ID
    embree-scene (convert-type context:embree-scene-handle RTCScene))
  (rtcIntersect1 embree-scene &ray-hit &intersect-arguments)
  (if (!= ray-hit.hit.geomID RTC_INVALID_GEOMETRY_ID) (return 0))
  (set
    out-path:delay (convert-type delay-sample sp-time-t)
    out-path:direction-source.x (* delta-x inverse-distance-meter)
    out-path:direction-source.y (* delta-y inverse-distance-meter)
    out-path:direction-source.z (* delta-z inverse-distance-meter)
    out-path:direction-receiver.x (* -1.0 delta-x inverse-distance-meter)
    out-path:direction-receiver.y (* -1.0 delta-y inverse-distance-meter)
    out-path:direction-receiver.z (* -1.0 delta-z inverse-distance-meter)
    out-path:triangle-index-chain 0
    out-path:triangle-index-count 0
    out-path:order 0
    out-path:path-type 0)
  (return 1))

(define (sp-reverb-early-paths-image context source receiver cutoff path-capacity out-path-list)
  (sp-time-t sp-reverb-early-context-t* sp-reverb-early-source-t* sp-reverb-early-receiver-t* sp-reverb-early-cutoff-t* sp-time-t sp-reverb-early-path-t*)
  (declare path sp-reverb-early-path-t count sp-time-t has-direct-path int)
  (set count 0)
  (if (= path-capacity 0) (return 0))
  (set has-direct-path (sp-reverb-early-direct-path context source receiver cutoff &path))
  (if (not has-direct-path) (return 0))
  (set (array-get out-path-list 0) path count 1)
  (return count))

(define (sp-reverb-early-paths-diffraction context edge-index-list edge-index-count cutoff)
  (sp-reverb-early-path-set-t sp-reverb-early-context-t* uint32-t* uint32-t sp-reverb-early-cutoff-t*)
  (declare path-set sp-reverb-early-path-set-t)
  (set path-set.path-list 0 path-set.path-count 0)
  (return path-set))
