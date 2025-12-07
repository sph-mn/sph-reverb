
#include <embree4/rtcore.h>
#include <embree4/rtcore_ray.h>
#include <embree4/rtcore_scene.h>
#include <embree4/rtcore_geometry.h>
void sp_reverb_early_context_init(sp_reverb_scene_t* scene, sp_reverb_early_context_t* out_context) {
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
  vertex_list = scene->geometry.vertex_list;
  index_list = scene->geometry.index_list;
  vertex_count = scene->geometry.vertex_count;
  index_count = scene->geometry.index_count;
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
  out_context->scene = scene;
  out_context->embree_device_handle = embree_device;
  out_context->embree_scene_handle = embree_scene;
}
void sp_reverb_early_context_shutdown(sp_reverb_early_context_t* context) {
  RTCScene embree_scene;
  RTCDevice embree_device;
  embree_scene = ((RTCScene)(context->embree_scene_handle));
  embree_device = ((RTCDevice)(context->embree_device_handle));
  rtcReleaseScene(embree_scene);
  rtcReleaseDevice(embree_device);
  context->scene = 0;
  context->embree_scene_handle = 0;
  context->embree_device_handle = 0;
}
int sp_reverb_early_path_compare_by_delay(const void* value_a, const void* value_b) {
  sp_reverb_early_path_t* path_a;
  sp_reverb_early_path_t* path_b;
  path_a = ((sp_reverb_early_path_t*)(value_a));
  path_b = ((sp_reverb_early_path_t*)(value_b));
  if (path_a->delay < path_b->delay) {
    return (-1);
  };
  if (path_a->delay > path_b->delay) {
    return (1);
  };
  return (0);
}
sp_bool_t sp_reverb_early_path_equal(sp_reverb_early_path_t* path_a, sp_reverb_early_path_t* path_b) {
  sp_time_t index;
  if (path_a->delay != path_b->delay) {
    return (0);
  };
  if (path_a->direction_receiver.x != path_b->direction_receiver.x) {
    return (0);
  };
  if (path_a->direction_receiver.y != path_b->direction_receiver.y) {
    return (0);
  };
  if (path_a->direction_receiver.z != path_b->direction_receiver.z) {
    return (0);
  };
  if (path_a->direction_source.x != path_b->direction_source.x) {
    return (0);
  };
  if (path_a->direction_source.y != path_b->direction_source.y) {
    return (0);
  };
  if (path_a->direction_source.z != path_b->direction_source.z) {
    return (0);
  };
  if (path_a->order != path_b->order) {
    return (0);
  };
  if (path_a->triangle_index_count != path_b->triangle_index_count) {
    return (0);
  };
  index = 0;
  while ((index < path_a->triangle_index_count)) {
    if ((path_a->triangle_index_chain)[index] != (path_b->triangle_index_chain)[index]) {
      return (0);
    };
    index = (index + 1);
  };
  if (path_a->path_type != path_b->path_type) {
    return (0);
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
  sp_reverb_early_path_set_t* path_set;
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
    path_set = &(path_set_list[set_index]);
    path_index = 0;
    while ((path_index < path_set->path_count)) {
      path_list[write_index] = (path_set->path_list)[path_index];
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
sp_bool_t sp_reverb_early_direct_path(sp_reverb_early_context_t* context, sp_reverb_early_source_t* source, sp_reverb_early_receiver_t* receiver, sp_reverb_early_cutoff_t* cutoff, sp_reverb_early_path_t* out_path) {
  RTCScene embree_scene;
  struct RTCRayHit ray_hit;
  struct RTCIntersectArguments intersect_arguments;
  sp_reverb_vec3_t source_position;
  sp_reverb_vec3_t receiver_position;
  sp_sample_t delta_x;
  sp_sample_t delta_y;
  sp_sample_t delta_z;
  sp_sample_t distance_meter;
  sp_sample_t inverse_distance_meter;
  sp_sample_t delay_sample;
  source_position = source->position_world;
  receiver_position = receiver->position_world;
  delta_x = (receiver_position.x - source_position.x);
  delta_y = (receiver_position.y - source_position.y);
  delta_z = (receiver_position.z - source_position.z);
  distance_meter = sqrt(((delta_x * delta_x) + (delta_y * delta_y) + (delta_z * delta_z)));
  if (distance_meter <= 0.0) {
    return (0);
  };
  inverse_distance_meter = (1.0 / distance_meter);
  delay_sample = (distance_meter * sp_reverb_sound_meter_sample);
  if (cutoff) {
    if (delay_sample > ((sp_sample_t)(cutoff->max_delay))) {
      return (0);
    };
    if (distance_meter > cutoff->max_path_length) {
      return (0);
    };
  };
  memset((&ray_hit), 0, (sizeof(struct RTCRayHit)));
  rtcInitIntersectArguments((&intersect_arguments));
  ray_hit.ray.org_x = source_position.x;
  ray_hit.ray.org_y = source_position.y;
  ray_hit.ray.org_z = source_position.z;
  ray_hit.ray.dir_x = ((float)((delta_x * inverse_distance_meter)));
  ray_hit.ray.dir_y = ((float)((delta_y * inverse_distance_meter)));
  ray_hit.ray.dir_z = ((float)((delta_z * inverse_distance_meter)));
  ray_hit.ray.tnear = 0.0f;
  ray_hit.ray.tfar = ((float)(distance_meter));
  ray_hit.ray.time = 0.0f;
  ray_hit.ray.mask = 0xffffffffu;
  ray_hit.ray.flags = 0;
  ray_hit.hit.geomID = RTC_INVALID_GEOMETRY_ID;
  ray_hit.hit.primID = RTC_INVALID_GEOMETRY_ID;
  (ray_hit.hit.instID)[0] = RTC_INVALID_GEOMETRY_ID;
  embree_scene = ((RTCScene)(context->embree_scene_handle));
  rtcIntersect1(embree_scene, (&ray_hit), (&intersect_arguments));
  if (ray_hit.hit.geomID != RTC_INVALID_GEOMETRY_ID) {
    return (0);
  };
  out_path->delay = ((sp_time_t)(delay_sample));
  out_path->direction_source.x = (delta_x * inverse_distance_meter);
  out_path->direction_source.y = (delta_y * inverse_distance_meter);
  out_path->direction_source.z = (delta_z * inverse_distance_meter);
  out_path->direction_receiver.x = (-1.0 * delta_x * inverse_distance_meter);
  out_path->direction_receiver.y = (-1.0 * delta_y * inverse_distance_meter);
  out_path->direction_receiver.z = (-1.0 * delta_z * inverse_distance_meter);
  out_path->triangle_index_chain = 0;
  out_path->triangle_index_count = 0;
  out_path->order = 0;
  out_path->path_type = 0;
  return (1);
}
sp_time_t sp_reverb_early_paths_image(sp_reverb_early_context_t* context, sp_reverb_early_source_t* source, sp_reverb_early_receiver_t* receiver, sp_reverb_early_cutoff_t* cutoff, sp_time_t path_capacity, sp_reverb_early_path_t* out_path_list) {
  sp_reverb_early_path_t path;
  sp_time_t count;
  int has_direct_path;
  count = 0;
  if (path_capacity == 0) {
    return (0);
  };
  has_direct_path = sp_reverb_early_direct_path(context, source, receiver, cutoff, (&path));
  if (!has_direct_path) {
    return (0);
  };
  out_path_list[0] = path;
  count = 1;
  return (count);
}
sp_reverb_early_path_set_t sp_reverb_early_paths_diffraction(sp_reverb_early_context_t* context, uint32_t* edge_index_list, uint32_t edge_index_count, sp_reverb_early_cutoff_t* cutoff) {
  sp_reverb_early_path_set_t path_set;
  path_set.path_list = 0;
  path_set.path_count = 0;
  return (path_set);
}
