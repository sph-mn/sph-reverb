
#include <sph-reverb/sph-reverb.h>
#include <sph-reverb/sph/test.h>
status_t test_sp_map_event(void) {
  status_declare;
  sp_time_t size;
  sp_block_t block;
  sp_sample_t* amod;
  sp_wave_event_config_t* config;
  sp_map_event_config_t* map_event_config;
  sp_declare_event(parent);
  sp_declare_event(child);
  error_memory_init(2);
  status_require((sp_wave_event_config_new((&config))));
  error_memory_add(config);
  status_require((sp_map_event_config_new((&map_event_config))));
  error_memory_add(map_event_config);
  size = (10 * _sp_rate);
  status_require((sp_path_samples2((&amod), size, (1.0), (1.0))));
  config->channel_count = 1;
  (config->channel_config)->frq = 300;
  (config->channel_config)->fmod = 0;
  (config->channel_config)->amp = 1;
  (config->channel_config)->amod = amod;
  child.start = 0;
  child.end = size;
  sp_wave_event((&child), config);
  status_require((sp_block_new(1, size, (&block))));
  map_event_config->event = child;
  map_event_config->map_generate = test_sp_map_event_generate;
  map_event_config->isolate = 1;
  parent.start = child.start;
  parent.end = child.end;
  sp_map_event((&parent), map_event_config);
  status_require(((parent.prepare)((&parent))));
  status_require(((parent.generate)(0, (size / 2), (&block), (&parent))));
  status_require(((parent.generate)((size / 2), size, (&block), (&parent))));
  (parent.free)((&parent));
  sp_block_free((&block));
  free(amod);
exit:
  if (status_is_failure) {
    error_memory_free;
  };
  status_return;
}
int main(void) {
  status_declare;
  test_helper_test_one(test_sp_map_event);
exit:
  test_helper_display_summary;
  return ((status.id));
}
