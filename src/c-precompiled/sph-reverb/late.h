typedef struct {
  sp_time_t period;
  sp_time_t decay;
} sp_reverb_late_mode_t;
typedef struct {
  sp_reverb_late_mode_t* mode_list;
  sp_time_t mode_count;
} sp_reverb_late_modal_set_t;
typedef struct {
  sp_reverb_late_mode_t* mode_list;
  sp_time_t mode_count;
  sp_sample_t* amplitude_list;
  sp_time_t* phase_list;
  sp_channel_count_t channel_count;
} sp_reverb_late_channel_modal_set_t;
typedef struct {
  sp_time_t* delays;
  sp_time_t delay_count;
  sp_sample_t* mix_row_major;
  sp_time_t mix_rows;
  sp_time_t mix_columns;
  sp_time_t* band_periods;
  sp_sample_t* band_gains;
  sp_time_t band_count;
  sp_sample_t strength;
  sp_sample_t* state_directions;
  uint8_t state_dimension_count;
} sp_reverb_late_config_t;
sp_reverb_late_modal_set_t sp_reverb_late_modal(sp_reverb_late_config_t* config);
void sp_reverb_late_modal_residues(sp_reverb_late_config_t* config, sp_reverb_late_modal_set_t* poles, sp_reverb_layout_t* layout, sp_reverb_position_t* position, sp_reverb_late_channel_modal_set_t* out_modes);