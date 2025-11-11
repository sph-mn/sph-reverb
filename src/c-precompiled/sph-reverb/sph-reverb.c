sp_reverb_response_t sp_reverb_late_lookup(sp_sample_t* frequency_list, sp_reverb_response_t* response_list, sp_time_t response_count, sp_sample_t query_frequency) {
  sp_time_t low;
  sp_time_t high;
  sp_time_t mid;
  sp_sample_t t;
  sp_reverb_response_t result;
  low = 0;
  high = (response_count - 1);
  if (query_frequency <= frequency_list[0]) {
    return ((response_list[0]));
  };
  if (query_frequency >= frequency_list[high]) {
    return ((response_list[high]));
  };
  while (((high - low) > 1)) {
    mid = (low + ((high - low) / 2));
    if (query_frequency < frequency_list[mid]) {
      high = mid;
    } else {
      low = mid;
    };
  };
  t = ((query_frequency - frequency_list[low]) / (frequency_list[high] - frequency_list[low]));
  result->gain = (((response_list[low]).gain) + (((response_list[high]).gain - (response_list[low]).gain) * t));
  result->decay = ((sp_time_t)((((sp_sample_t)((response_list[low]).decay)) + ((((sp_sample_t)((response_list[high]).decay)) - ((sp_sample_t)((response_list[low]).decay))) * t))));
  result->phase = ((sp_time_t)((((sp_sample_t)((response_list[low]).phase)) + ((((sp_sample_t)((response_list[high]).phase)) - ((sp_sample_t)((response_list[low]).phase))) * t))));
  return (result);
}
void sp_reverb_late_project(sp_reverb_response_t* response, sp_reverb_layout_t* layout, sp_sample_t* channel_gains, sp_time_t channel_count) {
  sp_time_t channel_index;
  sp_time_t basis_index;
  sp_time_t base_index;
  sp_sample_t sum;
  channel_index = 0;
  base_index = 0;
  while ((channel_index < layout->channel_count)) {
    sum = 0.0;
    basis_index = 0;
    while ((basis_index < layout->basis_length)) {
      sum = (sum + (layout->bases)[base_index]);
      base_index = (base_index + 1);
      basis_index = (basis_index + 1);
    };
    channel_gains[channel_index] = (response->gain * sum);
    channel_index = (channel_index + 1);
  };
}
void sp_reverb_late_table(sp_reverb_late_config_t* config, sp_time_t* frequency_list, sp_time_t frequency_count, sp_reverb_response_t* response_list) {
  sp_time_t delay_index;
  sp_time_t frequency_index;
  sp_time_t band_index;
  sp_time_t row_index;
  sp_time_t col_index;
  sp_time_t delay_sum;
  sp_time_t delay_mean;
  sp_sample_t row_energy_sum;
  sp_sample_t row_energy_mean;
  sp_sample_t mix_scale;
  sp_sample_t band_gain;
  sp_sample_t weight;
  delay_sum = 0;
  delay_index = 0;
  while ((delay_index < config->delay_count)) {
    delay_sum = (delay_sum + (config->delays)[delay_index]);
    delay_index = (delay_index + 1);
  };
  if (== ((config->delay_count), 0)) {
    delay_mean = 0;
  } else {
    delay_mean = (delay_sum / config->delay_count);
  };
  row_energy_sum = 0.0;
  row_index = 0;
  while ((row_index < config->mix_rows)) {
    sp_sample_t energy;
    sp_sample_t v;
    energy = 0.0;
    col_index = 0;
    while ((col_index < config->mix_columns)) {
      v = (config->mix_matrix_row_major)[((sp_time_t)(((row_index * config->mix_columns) + col_index)))];
      energy = (energy + (v * v));
      col_index = (col_index + 1);
    };
    row_energy_sum = (row_energy_sum + energy);
    row_index = (row_index + 1);
  };
  if (== ((config->mix_rows), 0)) {
    row_energy_mean = 1.0;
  } else {
    row_energy_mean = (row_energy_sum / ((sp_sample_t)(config->mix_rows)));
  };
  if (row_energy_mean > 0.0) {
    mix_scale = ((sp_sample_t)((1.0 / sqrt(row_energy_mean))));
  } else {
    mix_scale = 1.0;
  };
  frequency_index = 0;
  while ((frequency_index < frequency_count)) {
    if (== ((config->band_count), 0)) {
      band_gain = 0.0;
    } else {
      if (frequency_list[frequency_index] <= (config->band_frequencies)[0]) {
        band_gain = (config->band_gains)[0];
      } else {
        if (frequency_list[frequency_index] >= (config->band_frequencies)[(config->band_count - 1)()]) {
          band_gain = (config->band_gains)[(config->band_count - 1)];
        } else {
          band_index = 0;
          while (((config->band_frequencies)[(band_index + 1)] < frequency_list[frequency_index])) {
            band_index = (band_index + 1);
          };
          weight = (((sp_sample_t)((frequency_list[frequency_index] - (config->band_frequencies)[band_index]))) / ((sp_sample_t)(((config->band_frequencies)[(band_index + 1)] - (config->band_frequencies)[band_index]))));
          band_gain = ((config->band_gains)[band_index] + (((config->band_gains)[(band_index + 1)] - (config->band_gains)[band_index]) * weight));
        };
      };
    };
    (response_list[frequency_index]).gain = (config->strength * mix_scale);
    (response_list[frequency_index]).decay = ((sp_time_t)(band_gain));
    (response_list[frequency_index]).phase = delay_mean;
    frequency_index = (frequency_index + 1);
  };
}
