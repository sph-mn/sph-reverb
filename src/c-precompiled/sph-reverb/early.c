
#include <embree4/rtcore.h>
#include <embree4/rtcore_ray.h>
#include <embree4/rtcore_scene.h>
#include <embree4/rtcore_geometry.h>
void sp_reverb_early_build_scene(sp_reverb_early_geometry_t* geometry, sp_reverb_early_materials_t* materials, sp_reverb_early_scene_t* out_scene) {
  RTCDevice embree_device;
  RTCScene embree_scene;
  RTCGeometry embree_geometry;
  sp_reverb_vec3_t* vertex_list;
  uint32_t* index_list;
  uint32_t vertex_count;
  uint32_t index_count;
  uint32_t triangle_count;
  size_t vertex_stride;
  size_t index_stride;
  vertex_list = geometry->vertex_list;
  index_list = geometry->index_list;
  vertex_count = geometry->vertex_count;
  index_count = geometry->index_count;
  triangle_count = (index_count / ((uint32_t)(3)));
  vertex_stride = sizeof(sp_reverb_vec3_t);
  index_stride = (((size_t)(3)) * sizeof(uint32_t));
  embree_device = rtcNewDevice(0);
  embree_scene = rtcNewScene(embree_device);
  embree_geometry = rtcNewGeometry(embree_device, RTC_GEOMETRY_TYPE_TRIANGLE);
  rtcSetSharedGeometryBuffer(embree_geometry, RTC_BUFFER_TYPE_VERTEX, 0, RTC_FORMAT_FLOAT3, vertex_list, 0, vertex_stride, vertex_count);
  rtcSetSharedGeometryBuffer(embree_geometry, RTC_BUFFER_TYPE_INDEX, 0, RTC_FORMAT_UINT3, index_list, 0, index_stride, triangle_count);
  rtcCommitGeometry(embree_geometry);
  rtcAttachGeometry(embree_scene, embree_geometry);
  rtcReleaseGeometry(embree_geometry);
  rtcCommitScene(embree_scene);
  geometry->embree_scene_handle = embree_scene;
  out_scene->geometry = *geometry;
  out_scene->materials = *materials;
  out_scene->embree_device_handle = embree_device;
  out_scene->embree_scene_handle = embree_scene;
}
int sp_reverb_early_path_compare_by_delay(const void* a, const void* b) {
  sp_reverb_early_path_t* pa;
  sp_reverb_early_path_t* pb;
  pa = ((sp_reverb_early_path_t*)(a));
  pb = ((sp_reverb_early_path_t*)(b));
  if (pa->delay < pb->delay) {
    return (-1);
  };
  if (pa->delay > pb->delay) {
    return (1);
  };
  return (0);
}
int sp_reverb_early_path_equal(sp_reverb_early_path_t* a, sp_reverb_early_path_t* b) {
  sp_time_t index;
  if (a->delay != b->delay) {
    return (0);
  };
  if (a->gain != b->gain) {
    return (0);
  };
  if (a->direction.x != b->direction.x) {
    return (0);
  };
  if (a->direction.y != b->direction.y) {
    return (0);
  };
  if (a->direction.z != b->direction.z) {
    return (0);
  };
  if (a->order != b->order) {
    return (0);
  };
  if (a->wall_index_count != b->wall_index_count) {
    return (0);
  };
  index = 0;
  while ((index < a->wall_index_count)) {
    if ((a->wall_index_chain)[index] != (b->wall_index_chain)[index]) {
      return (0);
    };
    index = (index + 1);
  };
  if (a->band_count != b->band_count) {
    return (0);
  };
  index = 0;
  while ((index < a->band_count)) {
    if ((a->band_gain_list)[index] != (b->band_gain_list)[index]) {
      return (0);
    };
    index = (index + 1);
  };
  return (1);
}
sp_reverb_early_path_set_t sp_reverb_early_paths_union(sp_reverb_early_path_set_t* path_set_list, sp_time_t path_set_count) {
  sp_reverb_early_path_set_t result;
  sp_time_t set_index;
  sp_time_t path_index;
  sp_time_t write_index;
  sp_time_t unique_count;
  sp_time_t count;
  sp_reverb_early_path_t* path_list;
  sp_reverb_early_path_set_t* set;
  if (path_set_count == 0) {
    result.path_list = 0;
    result.path_count = 0;
    return (result);
  };
  result = path_set_list[0];
  path_list = result.path_list;
  write_index = result.path_count;
  set_index = 1;
  while ((set_index < path_set_count)) {
    set = &(path_set_list[set_index]);
    path_index = 0;
    while ((path_index < set->path_count)) {
      path_list[write_index] = (set->path_list)[path_index];
      write_index = (write_index + 1);
      path_index = (path_index + 1);
    };
    set_index = (set_index + 1);
  };
  result.path_count = write_index;
  count = result.path_count;
  if (count == 0) {
    return (result);
  };
  qsort(path_list, ((size_t)(count)), (sizeof(sp_reverb_early_path_t)), sp_reverb_early_path_compare_by_delay);
  unique_count = 1;
  write_index = 1;
  while ((write_index < count)) {
    if (!sp_reverb_early_path_equal((&(path_list[(write_index - 1)])), (&(path_list[write_index])))) {
      if (unique_count != write_index) {
        path_list[unique_count] = path_list[write_index];
      };
      unique_count = (unique_count + 1);
    };
    write_index = (write_index + 1);
  };
  result.path_count = unique_count;
  return (result);
}
int sp_reverb_early_direct_path(sp_reverb_early_scene_t* scene, sp_reverb_early_source_t* source, sp_reverb_early_receiver_t* receiver, sp_reverb_early_path_t* out_path) {
  RTCScene embree_scene;
  struct RTCRayHit ray_hit;
  struct RTCIntersectArguments args;
  sp_reverb_vec3_t source_position;
  sp_reverb_vec3_t receiver_position;
  sp_sample_t dx;
  sp_sample_t dy;
  sp_sample_t dz;
  sp_sample_t distance_m;
  sp_sample_t inv_distance_m;
  sp_sample_t delay_samples;
  source_position = source->position_world;
  receiver_position = receiver->position_world;
  dx = (receiver_position.x - source_position.x);
  dy = (receiver_position.y - source_position.y);
  dz = (receiver_position.z - source_position.z);
  distance_m = sqrt(((dx * dx) + (dy * dy) + (dz * dz)));
  if (distance_m <= 0.0) {
    return (0);
  };
  inv_distance_m = (1.0 / distance_m);
  delay_samples = (distance_m * sp_reverb_sound_meter_sample);
  memset((&ray_hit), 0, (sizeof(struct RTCRayHit)));
  rtcInitIntersectArguments((&args));
  ray_hit.ray.org_x = source_position.x;
  ray_hit.ray.org_y = source_position.y;
  ray_hit.ray.org_z = source_position.z;
  ray_hit.ray.dir_x = ((float)((dx * inv_distance_m)));
  ray_hit.ray.dir_y = ((float)((dy * inv_distance_m)));
  ray_hit.ray.dir_z = ((float)((dz * inv_distance_m)));
  ray_hit.ray.tnear = 0.0f;
  ray_hit.ray.tfar = ((float)(distance_m));
  ray_hit.ray.time = 0.0f;
  ray_hit.ray.mask = 0xffffffffu;
  ray_hit.ray.flags = 0;
  ray_hit.hit.geomID = RTC_INVALID_GEOMETRY_ID;
  ray_hit.hit.primID = RTC_INVALID_GEOMETRY_ID;
  (ray_hit.hit.instID)[0] = RTC_INVALID_GEOMETRY_ID;
  embree_scene = ((RTCScene)(scene->embree_scene_handle));
  rtcIntersect1(embree_scene, (&ray_hit), (&args));
  if (ray_hit.hit.geomID != RTC_INVALID_GEOMETRY_ID) {
    return (0);
  };
  out_path->delay = ((sp_time_t)(delay_samples));
  out_path->gain = inv_distance_m;
  out_path->direction.x = (dx * inv_distance_m);
  out_path->direction.y = (dy * inv_distance_m);
  out_path->direction.z = (dz * inv_distance_m);
  out_path->wall_index_chain = 0;
  out_path->wall_index_count = 0;
  out_path->order = 0;
  out_path->band_gain_list = 0;
  out_path->band_count = 0;
  return (1);
}
sp_reverb_early_path_set_t sp_reverb_early_paths_image(sp_reverb_early_scene_t* scene, sp_reverb_early_source_t* source, sp_reverb_early_receiver_t* receiver, sp_time_t max_order, sp_time_t path_cap) {
  sp_reverb_early_path_set_t result;
  sp_reverb_early_path_t* path_list;
  sp_reverb_early_path_t path;
  int has_direct_path;
  result.path_list = 0;
  result.path_count = 0;
  if (path_cap == 0) {
    return (result);
  };
  has_direct_path = sp_reverb_early_direct_path(scene, source, receiver, (&path));
  if (!has_direct_path) {
    return (result);
  };
  path_list = ((sp_reverb_early_path_t*)(malloc((sizeof(sp_reverb_early_path_t) * ((size_t)(1))))));
  if (path_list == 0) {
    return (result);
  };
  path_list[0] = path;
  result.path_list = path_list;
  result.path_count = 1;
  return (result);
}
sp_reverb_early_path_set_t sp_reverb_early_paths_beam(sp_reverb_early_scene_t* scene, sp_reverb_early_source_t* source, sp_reverb_early_receiver_t* receiver, sp_time_t max_order, sp_time_t path_cap, uint32_t* portal_index_list, uint32_t portal_index_count);
sp_reverb_early_path_set_t sp_reverb_early_paths_diffraction(sp_reverb_early_scene_t* scene, uint32_t* edge_index_list, uint32_t edge_index_count, sp_time_t* band_period_list, sp_time_t band_count);
sp_reverb_early_path_set_t sp_reverb_early_paths_cull(sp_reverb_early_path_set_t* path_set, sp_sample_t threshold, sp_time_t max_paths);
void sp_reverb_early_noise_partials_from_paths(sp_reverb_early_path_set_t* path_set, sp_reverb_layout_t* layout, sp_time_t band_period_start, sp_time_t band_period_end, sp_time_t duration, sp_reverb_early_noise_partial_t* out_partial_list, sp_time_t out_partial_capacity, sp_time_t* out_partial_count);
void sp_reverb_early_partials_from_paths(sp_reverb_early_path_set_t* path_set, sp_reverb_layout_t* layout, sp_reverb_sampled_partial_t* partial_list, sp_time_t partial_count, sp_reverb_early_partial_t* out_partial_list, sp_time_t out_partial_capacity, sp_time_t* out_partial_count);
void sp_reverb_early(sp_reverb_early_scene_t* scene, sp_reverb_early_source_t* source, sp_reverb_early_receiver_t* receiver, sp_reverb_layout_t* layout, sp_reverb_sampled_partial_t* partial_list, sp_time_t partial_count, sp_reverb_early_partial_t* out_partial_list, sp_time_t out_partial_capacity, sp_time_t* out_partial_count);